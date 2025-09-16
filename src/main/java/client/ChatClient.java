package client;

import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

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


    Terminal terminal;
    LineReader terminalReader;


    public ChatClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        try {
            this.terminal = TerminalBuilder.builder().system(true).build();
            this.terminalReader = LineReaderBuilder.builder().terminal(terminal).build();
        } catch (IOException e) {
            logger.severe("Could not initialize terminal");
        }
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
                String username = terminalReader.readLine();
                serverWriter.println("/newClient " + username);
            } while (serverReader.readLine().equalsIgnoreCase("Invalid Username"));

            Runnable serverListener = () -> {
                String message;
                try {
                    while (running) {
                        message = serverReader.readLine();
                        if (message.equalsIgnoreCase("/disconnect")) {
                            break;
                        } else {
                            terminalReader.printAbove(message);
                        }
                    }
                } catch (IOException ex) {
                    logger.warning("IOException from Server Reader: cnnection Lost");
                } finally {
                    running = false;
                    try {
                        terminal.close();
                    } catch (IOException e) {
                        logger.severe("Terminal could not be closed");
                    }
                    logger.info("Turning Off Server Listener: Client isn't listening to server anymore");
                    synchronized (this) {
                        notifyAll();
                    }
                }
            };

            Runnable terminalReader = () -> {
                String message = null;
                try {
                    while (running) {
                        message = this.terminalReader.readLine("> ");
                        if (message != null) {
                            if (!message.startsWith("/"))
                                serverWriter.println("/message " + message);
                            else
                                serverWriter.println(message);
                        }
                        message = null;
                    }
                    logger.info("Turning off Console Reader: Client isn't reading from the console");
                } catch (Exception ex) {
                    System.out.println("Exception from Terminal Reader: Not Reading From Terminal");
                }
            };

            executorService.submit(serverListener);
            executorService.submit(terminalReader);

            synchronized (this) {
                wait();
            }

        } catch (Exception e) {
            logger.info("Client failed to start or run");
        } finally {
            logger.info("Turning off the client: Client closed");
            if (executorService != null) {
                executorService.shutdown();
                try {
                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        logger.severe("ExecutorService did not terminate in time");
                    }
                } catch (InterruptedException e) {
                    //interrupt can't happen
                }
            }
        }
    }
}
