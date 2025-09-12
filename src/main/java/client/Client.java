package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    Socket socket;
    PrintWriter socketWriter;
    BufferedReader socketReader;
    Scanner consoleScanner;

    public Client() {
        try {
            socket = new Socket("localhost", 8082);
            socketWriter = new PrintWriter(socket.getOutputStream(), true);
            socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            consoleScanner = new Scanner(System.in);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.runClient();
    }


    public void runClient() {

        ServerListener serverListener = new ServerListener(socketReader);
        ConsoleReader consoleReader = new ConsoleReader(socketWriter, consoleScanner);
        Thread serverListenerThread = new Thread(serverListener);
        serverListenerThread.start();

        try {
            System.out.print("Enter your username: ");
            String username = consoleScanner.nextLine();
            socketWriter.println("newClient:"+ username);

        } catch (Exception e) {
            e.printStackTrace();
        }
        Thread consoleReaderThread = new Thread(consoleReader);
        consoleReaderThread.start();
    }
}



class ServerListener implements Runnable {

    private final BufferedReader socketReader;
    boolean running = true;

    ServerListener(BufferedReader socketReader) {
        this.socketReader = socketReader;
    }

    @Override
    public void run() {
        try {
            String message;
            while (running) {
                if ((message = socketReader.readLine()) != null) {
                    System.out.println(message);
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }
}

class ConsoleReader implements Runnable {

    PrintWriter socketWriter;
    Scanner consoleScanner;
    boolean running = true;

    ConsoleReader(PrintWriter socketWriter, Scanner consoleScanner) {
        this.socketWriter = socketWriter;
        this.consoleScanner = consoleScanner;
    }

    @Override
    public void run() {
        try {
            while(running) {
                String message = consoleScanner.nextLine();
                socketWriter.println("message:" + message);
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
    }
}