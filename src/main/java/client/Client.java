package client;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

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
        try (PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter consoleWriter = new PrintWriter(System.out, true);
        ) {
            while (running) {
                if ((message = reader.readLine()) != null) {
                    consoleWriter.println(message);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}