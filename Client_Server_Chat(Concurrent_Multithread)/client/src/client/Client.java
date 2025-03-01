package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 1555);
             BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter output = new PrintWriter(socket.getOutputStream(), true);
             Scanner scanner = new Scanner(System.in)) {

            String clientIdMessage = input.readLine();
            System.out.println(clientIdMessage);

            Thread readThread = new Thread(() -> {
                try {
                    while (true) {
                        String received = input.readLine();
                        if (received == null || received.equalsIgnoreCase("bye")) {
                            System.out.println("Server disconnected.");
                            System.exit(0);
                        }
                        System.out.println(received);
                    }
                } catch (IOException e) {
                    System.out.println("Error reading from server.");
                }
            });

            readThread.start();

            while (true) {
                System.out.print("YOU: ");
                String message = scanner.nextLine();
                output.println(message);
                if (message.equalsIgnoreCase("bye")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Error connecting to server.");
        }
    }
}
