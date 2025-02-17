package server1;
import java.net.*;
import java.io.*;

public class Server1 {
    public static void main(String args[]) throws IOException {
        // Register service on port 1254
        ServerSocket s = new ServerSocket(1254);
        System.out.println("Server is running and waiting for a connection...");

        while (true) { // Infinite loop to keep the server alive
            Socket s1 = s.accept(); // Wait and accept a connection
            System.out.println("Client connected!");

            // Get a communication stream associated with the socket
            OutputStream s1out = s1.getOutputStream();
            DataOutputStream dos = new DataOutputStream(s1out);

            // Send a string
            dos.writeUTF("Hi there");

            // Close the client-specific resources
            dos.close();
            s1out.close();
            s1.close();

            System.out.println("Message sent and client connection closed. Waiting for next client...");
        }
        // ServerSocket 's' is no longer closed here to keep accepting new clients
    }
}