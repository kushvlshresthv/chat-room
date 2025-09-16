package server;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());

    private final ServerSocket serverSocket;

    //everyting we modify the list, it entire underlying array is copied into a new one and then modification is applied on that new copy
    private final List<ConnectionHandler> connections = new CopyOnWriteArrayList<ConnectionHandler>();

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
                connections.add(connectionHandler);
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
        for (ConnectionHandler connectionHandler : connections) {
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
        for (ConnectionHandler connectionHandler : connections) {
            connectionHandler.sendMessage(message);
        }
    }

    public void removeConnection(ConnectionHandler handler) {
        connections.remove(handler);
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

        public ConnectionHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
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
            } catch (Exception e) {
                logger.info("Client " + username + " disconnected " + e.getMessage());
            }
        }


        public void handlePayLoad(String payload) {
            String command = payload.split(":")[0];

            switch (command) {
                case "/newClient": {
                    String usernameForNewUser = payload.split(":")[1];
                    handleNewClient(usernameForNewUser);
                    break;
                }
                case "/message": {
                    String message = payload.split(":")[1];
                    handleMessage(message);
                    break;
                }

                case "/changeUsername": {
                    String newUsername = payload.split(":")[1];
                    handleChangeUsername(newUsername);
                    break;
                }

                case "/disconnect": {
                    clientWriter.write("/disconnect");
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
            clientWriter.println("Welcome to the chatroom " + usernameForNewUser);
            broadcast("'" + usernameForNewUser + "' has just joined the chatroom");
            this.username = usernameForNewUser;
        }

        void handleMessage(String message) {
            broadcast(username + ": " + message);
        }

        void handleChangeUsername(String newUsername) {

        }


        /**
         * used to send the message to client Socket
         */
        public void sendMessage(String message) {
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
        }
    }
}
