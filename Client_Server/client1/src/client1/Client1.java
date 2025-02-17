package client1;
import java.net.*;
import java.io.*;

public class Client1 {
    public static void main(String args[]) throws IOException {
        // Open connection to server on port 1254
        Socket s1 = new Socket("localhost", 1254);
        System.out.println("Connected to the server!");

        // Get an input file handle from the socket and read the input
        InputStream s1In = s1.getInputStream();
        DataInputStream dis = new DataInputStream(s1In);

        // Read the message from the server
        String st = dis.readUTF();
        System.out.println(st);

        // Close the connection
        dis.close();
        s1In.close();
        s1.close();

        System.out.println("Connection closed.");
    }
}
