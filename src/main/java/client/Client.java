package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    Socket socket;
    String message;
    boolean running = true;

    public Client() {
        try {
            socket = new Socket("localhost", 8082);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Client client = new Client();
        client.runClient();
    }


    public void runClient() {
        try (PrintWriter socketWriter = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader socketReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter consoleWriter = new PrintWriter(System.out, true);
             Scanner scanner = new Scanner(System.in);
        ) {

            consoleWriter.print("Enter your username: ");
            consoleWriter.flush();
            String username = scanner.nextLine();
            socketWriter.println(username);

            while (running) {
                if ((message = socketReader.readLine()) != null) {
                    consoleWriter.println(message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}