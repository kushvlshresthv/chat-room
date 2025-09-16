package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;


public class ChatClient {
    private static final Logger logger = Logger.getLogger(ChatClient.class.getName());

    private volatile boolean running = true;
    private final String hostname;
    private final int port;
    ExecutorService executorService;

    public ChatClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
    }

    public static void main(String[] args) {
        ChatClient client = new ChatClient("localhost", 8082);
        client.runClient();
    }


    public void runClient() {
        executorService = Executors.newFixedThreadPool(2);
        try (
                Socket clientSocket = new Socket(hostname, port);
                PrintWriter serverWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader serverReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedReader consoleBufferedReader = new BufferedReader(new InputStreamReader(System.in))) {

            do {
                System.out.print("Enter your username: ");
                String username = consoleBufferedReader.readLine();
                serverWriter.println("/newClient " + username);
            } while(serverReader.readLine().equalsIgnoreCase("Invalid Username"));

            Runnable serverListener = () -> {
                String message;
                try {
                    while (running) {
                        message = serverReader.readLine();
                        if (message.equalsIgnoreCase("/disconnect")) {
                            synchronized (this) {
                                notifyAll();
                                running = false;
                                break;
                            }
                        } else {
                            System.out.println(message);
                        }
                    }
                } catch (IOException ex) {
                    if (!clientSocket.isClosed()) {
                        logger.warning("IOException from Server Reader: Connection Lost");
                    }
                } finally {
                    logger.info("Turning Off Server Listener: Client isn't listening to server anymore");
                }
            };

            Runnable consoleReader = () -> {
                String message = null;
                try {
                    while (running) {
                        if(consoleBufferedReader.ready()) {
                           message = consoleBufferedReader.readLine();
                        }

                        if(message != null) {
                            if (!message.startsWith("/"))
                                serverWriter.println("/message " + message);
                            else
                                serverWriter.println(message);
                        }
                        message = null;
                    }
                    logger.info("Turning off Console Reader: Client isn't reading from the console");
                } catch(IllegalStateException | IOException ex) {
                    System.out.println("IOEException from Console Scanner: Not Reading From Console");
                }
            };

            executorService.submit(serverListener);
            executorService.submit(consoleReader);

            synchronized (this) {
                wait();
            }

        } catch (Exception e) {
            logger.info("Client failed to start or run");
        } finally {
            logger.info("Turning off the client: Client closed");
            if(executorService != null) {
                executorService.shutdown();
                try {
                if(!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                   logger.severe("ExecutorService did not terminate in time");
                }
                } catch(InterruptedException e) {
                    //interrupt can't happen
                }
            }
        }
    }
}
