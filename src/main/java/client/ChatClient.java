package client;

import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.impl.completer.StringsCompleter;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.utils.AttributedStyle;
import org.jline.utils.InfoCmp;
import utils.ColorPrint;

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
    private volatile String myUsername;



    Terminal terminal;
    LineReader terminalReader;

    public ChatClient(String hostname, int port) {
        this.hostname = hostname;
        this.port = port;
        try {
            Completer completer = new StringsCompleter("/disconnect", "/changeUsername", "/onlineCount");
            this.terminal = TerminalBuilder.builder().system(true).build();
            this.terminalReader = LineReaderBuilder.builder().terminal(terminal).completer(completer).build();
        } catch (IOException e) {
            logger.severe("Could not initialize terminal");
        }
    }


    public static void main(String[] args) {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%4$s: %5$s%n");
        ChatClient client = new ChatClient("localhost", 8082);
        client.runClient();
    }


    public void runClient() {
        //shared Lock object
        final Object lock = new Object();

        executorService = Executors.newFixedThreadPool(2);
        try (
                Socket clientSocket = new Socket(hostname, port);
                PrintWriter serverWriter = new PrintWriter(clientSocket.getOutputStream(), true);
                BufferedReader serverReader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                BufferedReader consoleBufferedReader = new BufferedReader(new InputStreamReader(System.in))) {


            //initial authentication

            do {
                String reply;
                String newUsername;
                System.out.print("Enter your username: ");
                newUsername = terminalReader.readLine();
                serverWriter.println("/newClient " + newUsername);
                reply = serverReader.readLine();

                if(reply.contains(":")) {

                    String typeOfResponse = reply.substring(0, reply.indexOf(":"));
                    String responseBody = reply.substring(reply.indexOf(":") + 1);

                    if(typeOfResponse.equalsIgnoreCase("Error")) {
                        ColorPrint.printAtCenterWithBox(terminalReader, responseBody.trim(), AttributedStyle.RED);
                    }

                    else if(typeOfResponse.equalsIgnoreCase("Success")) {
                        //print the welcome message
                        ColorPrint.printAtCenterWithBox(terminalReader, responseBody, AttributedStyle.YELLOW /*orange color*/);

                        setMyUsername(newUsername);
                        break;
                    }
                }
            } while (true);



            Runnable serverListenerTask = () -> {
                String response;
                try {
                    while (running) {
                        response = serverReader.readLine();
                        if(response == null) {
                            break;
                        }
                        String typeOfResponse;
                        String responseBody;

                        response = response.trim();

                        //if the server issues disconnect command
                        if(response.equalsIgnoreCase("disconnect"))
                            break;

                        //when the response does not contain any 'type'
                        if(!response.contains(":")) {
                            ColorPrint.printAtCenterWithBox(terminalReader, response, 208 /*orange color*/);
                            continue;
                        }


                        //extracting the type of response and response body
                        int colonIndex = response.indexOf(":");
                        typeOfResponse = response.substring(0, colonIndex).trim();
                        responseBody = response.substring(colonIndex +1).trim();


                        switch (typeOfResponse) {
                            case "Error":
                            case "Disconnect": {
                                ColorPrint.printAtCenterWithBox(terminalReader, responseBody, AttributedStyle.RED);
                                break;
                            }
                            case "Message": {
                                String username = responseBody.split(":")[0].trim();
                                int usernameColor = Integer.parseInt(responseBody.split(":")[1].trim());

                                String actualmessage = responseBody.split(":")[2].trim();

                                ColorPrint.printUserMessage(terminalReader, username, usernameColor ,actualmessage);

                                break;
                            }

                            case "UsernameChanged": {
                                setMyUsername(responseBody.split(":")[0].trim());
                                String actualMessage = responseBody.split(":")[1].trim();
                                ColorPrint.printAtCenterWithBox(this.terminalReader, actualMessage, AttributedStyle.YELLOW);

                                //terminalReaderTask is waiting whether the change is success or failure to display the messge prompt.
                                synchronized (lock) {
                                    lock.notifyAll();
                                }
                                break;
                            }

                            case "UsernameChangeFailed": {
                                ColorPrint.printAtCenterWithBox(terminalReader, responseBody, AttributedStyle.RED);
                                synchronized (lock) {
                                    lock.notifyAll();
                                }
                                break;
                            }

                            case "OnlineCount": {
                                if(responseBody.equals("1")) {
                                    ColorPrint.printAtCenterWithBox(terminalReader, "Only you are in the chat room", 208);
                                } else {
                                    ColorPrint.printAtCenterWithBox(terminalReader, responseBody + " people are in the chat room", 208);
                                }
                                break;
                            }
                           default: {
                                ColorPrint.printAtCenterWithBox(terminalReader, response, 208 /*orange color*/);
                            }
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

            Runnable terminalReaderTask = () -> {
                String message = null;
                try {
                    while (running) {
                        String prompt = "\n" + myUsername + "> ";
                        message = this.terminalReader.readLine(prompt);

                        for(int i = 0; i<2; i++) {
                            // Erase the previous line (the input line)
                            terminal.puts(InfoCmp.Capability.cursor_up);   // move up
                            terminal.puts(InfoCmp.Capability.carriage_return); // go to start of line
                            terminal.puts(InfoCmp.Capability.clr_eol);    // clear it
                            terminal.flush();
                        }

                        if (message != null) {
                            if (!message.startsWith("/")) {
                                serverWriter.println("/message " + message);
                                ColorPrint.printMyMessage(this.terminalReader, message);
                            } else if(message.contains("/changeUsername")) {
                                serverWriter.println(message);
                                synchronized (lock) {
                                    lock.wait();
                                    //thread waits until changeUsername is accepted by the server
                                    //if this is not done, it renders the old username for prompting input
                                }
                            }
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

            executorService.submit(serverListenerTask);
            executorService.submit(terminalReaderTask);

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
    private void setMyUsername(String username) {
        this.myUsername = username;
    }
}
