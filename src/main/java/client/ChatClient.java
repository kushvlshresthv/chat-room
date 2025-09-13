package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;


public class ChatClient implements AutoCloseable {
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());

    private final String hostname;
    private final int port;
    boolean running = true;
    ExecutorService executorService;

    public ChatClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public static void main(String[] args) {
        try (ChatClient client = new ChatClient("localhost", 8082);
        ) {
            client.runClient();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Client failed to start or run", e);
        }
    }


    public void runClient() {
        executorService = Executors.newFixedThreadPool(2);
        try (
                Socket clientSocket = new Socket(hostname, port);
                PrintWriter socketWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader socketReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                Scanner consoleScanner = new Scanner(System.in)) {

            System.out.print("Enter your username: ");
            String username = consoleScanner.nextLine();
            socketWriter.println("newClient:" + username);

            Runnable serverListener = () -> {
                String message;
                try {
                    while (running) {
                        if ((message = socketReader.readLine()) != null) {
                            System.out.println(message);
                        }
                    }
                } catch(IOException ex) {
                    if(!clientSocket.isClosed()) {
                        logger.warning("Connectio nLost");
                    }
                } finally {
                    logger.info("server listener is shutting down");
                }
            };

            Runnable consoleReader = ()-> {
                try {
                    while (running) {
                        String message = consoleScanner.nextLine();
                        socketWriter.println("message:" + message);
                    }
                } catch (Exception e) {
                    logger.info("Console reader shutting down");
                }
            };

        executorService.submit(serverListener);
        executorService.submit(consoleReader);

        //TODO: see how wait() works and when server sends sometype of command, this thread should be interrupted which initiates the shutdown as it is an Autocloseable resource.
        synchronized(this) {
            try {
                super.wait();
            } catch(InterruptedException e) {
                //handle the exception
            }
        }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void close() {
        logger.info("Closing Chat Client");
        running = false;
        if(executorService != null) {
            executorService.shutdown();
        }
    }
}
