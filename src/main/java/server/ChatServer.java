package server;


import utils.ColorAssigner;
import utils.CustomColors;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

/**
 * <p>This is the Server which when instantiated attaches to port 8082 and starts accepting requests</p>
 *
 * <p> For every new incoming request, it instantiates a new ConnectionHandler which is responsinble for handling the request</p>
 *
 * <p>After instantiating the ConnectionHandler, it assigns a thread from ThreadPool to handle the connection</p>
 */

public class ChatServer implements AutoCloseable {
    //constants:
    private final int MAX_USERNAME_SIZE = 10;


    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());

    private final ServerSocket serverSocket;

    // CopyOnWriteArrayList: modifications (add/remove) create a new array copy.
    // Threads already iterating see the old snapshot; new iterations/readers see the updated list.
    private final ConcurrentHashMap<String, ConnectionHandler> connections = new ConcurrentHashMap<>();

    private final ExecutorService executorService;

    public ChatServer(int port) throws IOException {
        serverSocket = new ServerSocket(8082);
        executorService = Executors.newCachedThreadPool();
        logger.info("Server started on port: " + port);
    }


    public static void main(String[] args) {
        try (ChatServer server = new ChatServer(8082)) {
            server.runServer();
        } catch (Exception e) {
            logger.severe("ERROR: Failed to start the server");
        }
    }


    public void runServer() {
        try {
            while (!serverSocket.isClosed()) {
                Socket clientSocket = serverSocket.accept();
                logger.info("New client connected: " + clientSocket.getInetAddress());
                ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket);
                executorService.execute(connectionHandler);
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                logger.severe("Error: IOEException while accepting client connection");
            }
        } finally {
            logger.info("Server is no longer accepting new connections");
        }
    }

    @Override
    public void close() {
        //first close all the connections
        for (ConnectionHandler connectionHandler : connections.values()) {
            connectionHandler.close();
        }
        //close the Thread Pool
        executorService.shutdown();
        try {
            //close the server
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            logger.severe("Error while attempting to close the server");
        }
    }


    public void broadcast(String message) {
        for (ConnectionHandler connectionHandler : connections.values()) {
            connectionHandler.send(message);
        }
    }

    public void broadcastExceptFor(String message, ConnectionHandler ignoreThisClient) {
        for (ConnectionHandler connectionHandler : connections.values()) {
            if(connectionHandler != ignoreThisClient)
                connectionHandler.send(message);
        }
    }


    public void removeConnection(ConnectionHandler handler) {
        connections.remove(handler.username);
    }

    /**
     * adds the parameter to the HashMap with its username as key
     */
    public void addConnection(ConnectionHandler handler) {
        connections.put(handler.username, handler);
    }


    /**
     * NOTE: The reason inner class was used for Connection Handler instead of separate class because some instance methods such as 'broadcast()' has to be accessed by Connection Handler instances
     *
     * <h2>What this class does</h2>
     * <p>First the server greets the client by sending 'Welcome to the chat room'</p>
     *
     * <p>Send the notification to all the other connected members that new person has joined the chat room</p>
     *
     */

    public class ConnectionHandler implements Runnable, AutoCloseable {
        final private Socket clientSocket;
        String username;
        PrintWriter clientWriter;
        private boolean isNew;
        private boolean isAdmin  =false;
        int usernameColor;

        public ConnectionHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            this.isNew = true;
        }

        @Override
        public void run() {
            try (
                    BufferedReader clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    PrintWriter clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
            ) {
                this.clientWriter = clientWriter;
                String payload;
                while ((payload = clientReader.readLine()) != null) {
                    handlePayLoad(payload);
                }
            } catch (IOException e) {
                logger.info("Client " + username + " disconnected " + e.getMessage());
                close();
            }
        }


        public void handlePayLoad(String payload) {
            payload = payload.trim();
            String command = null;
            String body = null;

            int spaceIndex = payload.indexOf(" ");

            if(payload.isEmpty()) {
                clientWriter.println("Invalid payload: format should be 'command <space> body'");
                return;
            }

            //extracting the command and body
            //some payoads may not have body such as '/disconnect'
            if(spaceIndex != -1 && !payload.substring(spaceIndex + 1).trim().isEmpty()) {
                command = payload.substring(0, spaceIndex).trim();
                body = payload.substring(spaceIndex + 1).trim();
            } else {
                command = payload.trim();
            }

            if(command.equals("/adminLogin")) {
                String username;
                String password;
                if(body != null && body.contains("--")) {
                    username = body.trim().split("--")[0];
                    password = body.trim().split("--")[1];
                    handleAdminLogin(username, password);
                    return;
                }
                send("Error: /adminLogin requires credentials: <username>--<password>");
                return;
            }


            //this is done to prevent client to explicitly execute /isNew command even when it is not a new client
            if(isNew) {
                if(command.equals("/newClient")) {
                    handleNewClient(body);
                    return;
                }
                //the cient is new but trying to execute any other command
                send("Register with /newClient command with a valid username");
                return;
            }

            switch (command) {
                case "/message": {
                    handleMessage(body);
                    break;
                }

                case "/changeUsername": {
                    if(isAdmin) {
                        send("UsernameChangeFailed: admin can't change their username");
                        break;
                    }
                    handleChangeUsername(body);
                    break;
                }

                case "/ban": {
                    if(isAdmin) {
                        handleUserBan(body);
                        break;
                    } else {
                        send("Error: only admins can use /ban command");
                        break;
                    }
                }

                case "/disconnect": {
                    //send /disconnect so that client can initate the disconnect process
                    send("/disconnect");
                    broadcastExceptFor("Disconnect: " + username + " has left the chat", this);
                    close();
                    break;
                    //this closes the socket, and since the socket is closed, run() faces an exception which logs Client Disconnected as a part of error handling.
                }

                case "/onlineCount": {
                    send("OnlineCount: " + connections.size());
                    break;
                }

                case "/onlineList": {
                    StringBuilder list = new StringBuilder();
                    int count = 1;
                    for(String name: connections.keySet()) {
                        list.append(count).append(". ").append(name).append("--");
                        count++;
                    }
                    send("OnlineList: " + list.substring(0, list.length() - 2/*remove the last '--'*/));
                    break;
                }

                case "/help": {
                    StringBuilder helps = new StringBuilder();
                    int count = 1;
                    helps.append("Help: ");
                    helps.append("/onlineCount    : check how many people are online");

                    helps.append("--");
                    helps.append("/onlineList     : list the online usernames");

                    helps.append("--");
                    helps.append("/disconnect     : leave the chat roomt");

                    helps.append("--");
                    helps.append("/changeUsername <newUsername>: changes the username");

                    send(helps.toString());
                    break;
                }

                default: {
                    clientWriter.println("Wrong Command");
                }
            }
        }



        void handleAdminLogin(String username, String password) {
            if(!(username.equals("admin") &&  password.equals("admin"))) {
               send("Error: Incorrect Credentials");
               return;
            }
            if(connections.containsKey("admin")) {
                send("Error: Admin is already logged in");
                return;
            }
            this.username = "admin";
            this.usernameColor = CustomColors.BRIGHT_RED;
            isAdmin = true;
            addConnection(this);
            send("Success: Logged in as admin");
            isNew = false;
        }

        void handleUserBan(String username) {
            if(username != null && username.trim().isEmpty()) {
                send("Error: Insert a valid username");
                return;
            }

            if(!connections.containsKey(username)) {
                send("Error: The provided username is not in the chat room");
                return;
            }

            ConnectionHandler userToBan = connections.get(username);
            userToBan.send("Error: You have been banned");
            userToBan.close();
            broadcast("'"+ username +"' has been banned by admin");
        }

        void handleNewClient(String usernameForNewUser) {
            String result = checkUsernameValidity(usernameForNewUser);
            if(result != null) {
                send("Error: " + result);
                return;
            }

            //valid username:
            send("Success: WELCOME TO THE CHATROOM " + usernameForNewUser);
            broadcastExceptFor("'" + usernameForNewUser + "' has joined the chat", this);
            this.username = usernameForNewUser;
            this.usernameColor = ColorAssigner.getNextColor();
            addConnection(this);
            this.isNew = false;
        }

        void handleMessage(String message) {
            if(message == null) {
                send("Error: Please enter a valid message");
                return;
            }
            broadcastExceptFor("Message: " + username + ": " + usernameColor +": " + message, this);
        }

        void handleChangeUsername(String newUsername) {
            //To see why UsernameChangeFailed used instead of 'Error', see Client implementation(Thread waiting issue)
            String result = checkUsernameValidity(newUsername);
            if(result != null) {
                send("UsernameChangeFailed: " + result);
                return;
            }

            removeConnection(this); //old username as key is removed
            String oldUsername = username;
            this.username = newUsername;
            addConnection(this);
            broadcastExceptFor("'" + oldUsername+"'" + " changed their username to '" + newUsername + "'", this);
            send("UsernameChanged: " + newUsername + ": Username successfully changed to '" + newUsername + "'");
        }

        /**
         * returns error message if the validation failed,
         * returns null, if the validation succeeds
         */
        private String checkUsernameValidity(String username) {
            if(username == null || username.trim().isEmpty()) {
                return "Username cannot be empty";
            } else if(username.length() > MAX_USERNAME_SIZE) {
                return "Username too long [10 characters max]";
            } else if(connections.containsKey(username)) {
                return "Username is already in use";
            } else if(username.equalsIgnoreCase("admin")) {
                return "This username is reserved";
            } else if(username.contains("-")) {
                return "Username can't contain '-' character";
            }

            else {
                return null;
            }
        }



        /**
         * used to send the message to client Socket
         */
        public void send(String message) {
            clientWriter.println(message);
        }

        @Override
        public void close() {
            //closing the client socket from the server side
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientWriter.close();
                    clientSocket.close();
                } catch (IOException e) {
                    logger.warning("Error while attempting to close the client socket associated with the username:" + username);
                }
            }
            removeConnection(this);
        }
    }
}
