package server;


import utils.ColorAssigner;

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

            //this is done to prevent client to explicitly execute /isNew command even when it is not a new client
            if(isNew) {
                if(command.equals("/newClient")) {
                    handleNewClient(body);
                    return;
                }
                //the cient is new but trying to execute any other command
                clientWriter.println("Before using other commands, register with /newClient command with a valid username");
                return;
            }

            switch (command) {
                case "/message": {
                    handleMessage(body);
                    break;
                }

                case "/changeUsername": {
                    handleChangeUsername(body);
                    break;
                }

                case "/disconnect": {
                    send("/disconnect");
                    broadcastExceptFor("Disconnect: " + username + " has left the chat", this);
                    close();
                    break;
                    //this closes the socket, and since the socket is closed, run() faces an exception which logs Client Disconnected as a part of error handling.
                }

                default: {
                    clientWriter.println("Wrong Command");
                }
            }
        }

        void handleNewClient(String usernameForNewUser) {
            if(usernameForNewUser == null || usernameForNewUser.trim().isEmpty()) {
                send("Error: Invalid Username");
                return;
            }

            if(usernameForNewUser.length() > MAX_USERNAME_SIZE) {
                send("Error: Username too long [10 characters max]");
                return;
            }

            if(connections.containsKey(usernameForNewUser)) {
                send("Error: Username is already in use");
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
            //To see why UsernameChangeFailed used instead of 'Error', see Client implementation
            if(newUsername == null || newUsername.trim().isEmpty()) {
                send("UsernameChangeFailed: Username shouldn't be empty");
                return;
            }


            if(newUsername.length() > MAX_USERNAME_SIZE) {
                send("UsernameChangeFailed: Username too long [10 characters max]");
                return;
            }

            if(connections.containsKey(newUsername)) {
                send("UsernameChangeFailed: Username is already in use");
                return;
            }

            String oldUsername = username;
            this.username = newUsername;
            removeConnection(this); //old username as key is removed
            addConnection(this);
            broadcastExceptFor("'" + oldUsername+"'" + " changed their username to '" + newUsername + "'", this);
            send("UsernameChanged: " + newUsername + ": Username successfully changed to '" + newUsername + "'");
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
