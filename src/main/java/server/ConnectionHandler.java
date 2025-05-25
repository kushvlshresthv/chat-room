

//package server;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.io.PrintWriter;
//import java.net.Socket;
//
///**
//    <p>First the server greets the client by sending 'Welcome to the chat room'</p>
//
// */
//
//
//public class ConnectionHandler implements Runnable {
//    final private Socket clientSocket;
//    PrintWriter writer;
//    BufferedReader reader;
//
//
//    public ConnectionHandler(Socket clientSocket) {
//        this.clientSocket = clientSocket;
//    }
//
//
//
//    @Override
//    public void run() {
//       try
//        {
//            writer = new PrintWriter(clientSocket.getOutputStream(), true);
//            reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
//            writer.println("Welcome to the chatroom");
//       } catch(Exception e) {
//            e.printStackTrace();
//       }
//    }
//
//
//
//    public void shutdown() {
//        if(clientSocket != null && !clientSocket.isClosed()) {
//            try {
//                writer.close();
//                reader.close();
//                clientSocket.close();
//            } catch(Exception e) {
//               e.printStackTrace();
//            }
//        }
//    }
//
//}
