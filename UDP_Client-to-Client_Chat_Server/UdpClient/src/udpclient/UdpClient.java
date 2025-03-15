package udpclient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class UdpClient {
    private static final String SERVER_IP = "127.0.0.1"; // Change this if needed
    private static final int SERVER_PORT = 5001;

    public static void main(String[] args) {
        try (DatagramSocket clientSocket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {

            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
            byte[] receiveBuffer = new byte[1024];

            System.out.println("Connected to UDP chat server at " + SERVER_IP + ":" + SERVER_PORT);

            // Thread to listen for incoming messages
            Thread listenerThread = new Thread(() -> {
                try {
                    while (true) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        clientSocket.receive(receivePacket);
                        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());
                        System.out.println("\n" + receivedMessage);
                        System.out.print("You: ");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            listenerThread.start();

            while (true) {
                System.out.println("\nChoose an option:");
                System.out.println("1. Send Message to Everyone");
                System.out.println("2. View Connected Clients");
                System.out.println("3. Send Private Message");
                System.out.println("4. Exit");
                System.out.print("Your Choice: ");

                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                switch (choice) {
                    case 1:
                        System.out.print("You: ");
                        String message = scanner.nextLine();
                        sendMessage(clientSocket, "MSG:" + message, serverAddress);
                        break;

                    case 2:
                        sendMessage(clientSocket, "LIST", serverAddress);
                        break;

                    case 3:
                        System.out.print("Enter client (IP:Port) to message: ");
                        String targetClient = scanner.nextLine();
                        System.out.print("Your message: ");
                        String privateMessage = scanner.nextLine();
                        sendMessage(clientSocket, "PM:" + targetClient + ":" + privateMessage, serverAddress);
                        break;

                    case 4:
                        sendMessage(clientSocket, "EXIT", serverAddress);
                        System.out.println("Exiting...");
                        clientSocket.close();
                        return;

                    default:
                        System.out.println("Invalid choice. Please enter a number between 1-4.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void sendMessage(DatagramSocket socket, String message, InetAddress address) {
        try {
            byte[] messageData = message.getBytes();
            DatagramPacket sendPacket = new DatagramPacket(messageData, messageData.length, address, SERVER_PORT);
            socket.send(sendPacket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
