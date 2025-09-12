package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
*<p>This is the Server which when instantiated attaches to port 8082 and starts accepting requests</p>

*<p> For every new incoming request, it instantiates a new ConnectionHandler which is responsinble for handling the request</p>

* <p>After instantiating the ConnectionHandler, it assigns a thread from ThreadPool to handle the connection</p>
 */

public class Server {
    private ServerSocket serverSocket;
    private final List<ConnectionHandler> connections = new ArrayList<ConnectionHandler>();
    PrintWriter writer;
    BufferedReader reader;
    boolean done = false;

    ExecutorService pool;

    public Server() {
        try {
            serverSocket = new ServerSocket(8082);
            pool = Executors.newCachedThreadPool();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void runServer() {
        try {
            while (!done) {
                Socket clientSocket = serverSocket.accept();
                ConnectionHandler connectionHandler = new ConnectionHandler(clientSocket);
                connections.add(connectionHandler);
                pool.execute(connectionHandler);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdown() {
        done = true;

        try {
            //first close all the connections
            for (ConnectionHandler connectionHandler : connections) {
                connectionHandler.shutdown();
            }

            //close the Thread Pool
            pool.shutdown();

            //close the server
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.runServer();
    }

    public void broadcast(String message) {
        for(ConnectionHandler connectionHandler : connections) {
            connectionHandler.sendMessage(message);
        }
    }


    /**
     * NOTE: The reason inner class was used for Connection Handler instead of separate class because some instance methods such as 'broadcast()' has to be accessed by Connection Handler instances

     * <h2>What this class does</h2>
     * <p>First the server greets the client by sending 'Welcome to the chat room'</p>

     * <p>Send the notification to all the other connected members that new person has joined the chat room</p>
     *
     */
    class ConnectionHandler implements Runnable {
        boolean running = true;
        final private Socket clientSocket;
        PrintWriter clientWriter;
        BufferedReader clientReader;
        String username;

        public ConnectionHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
            try {
                clientWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                clientReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
           try{
               System.out.println("Client Connection Received: ");

               while(running) {
                   String payload = clientReader.readLine();
                   String command = payload.split(":")[0];

                   switch(command) {
                       case "newClient": {
                           String usernameForNewUser = payload.split(":")[1];
                           handleNewClient(usernameForNewUser);
                           break;
                       }
                       case "message": {
                           String message = payload.split(":")[1];
                           handleMessage(message);
                           break;
                       }

                       case "changeUsername": {
                           String newUsername = payload.split(":")[1];
                           handleChangeUsername(newUsername);
                           break;
                       }
                   }
               }

            } catch(Exception e) {
                e.printStackTrace();
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
         *  used to send the message to client Socket
         */
        public void sendMessage(String message) {
            clientWriter.println(message);
        }

        public void shutdown() {
            if (clientSocket != null && !clientSocket.isClosed()) {
                try {
                    clientWriter.close();
                    clientReader.close();
                    clientSocket.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}