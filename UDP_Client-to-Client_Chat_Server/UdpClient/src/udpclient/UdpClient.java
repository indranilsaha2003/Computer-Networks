package udpclient;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Scanner;

public class UdpClient {
    private static final String SERVER_IP = "127.0.0.1"; 
    private static final int SERVER_PORT = 5001;

    public static void main(String[] args) {
        try (DatagramSocket clientSocket = new DatagramSocket();
             Scanner scanner = new Scanner(System.in)) {

            InetAddress serverAddress = InetAddress.getByName(SERVER_IP);
            byte[] receiveBuffer = new byte[1024];

            System.out.println("Connected to UDP chat server at " + SERVER_IP + ":" + SERVER_PORT);
            System.out.println("Type messages and press Enter to send:");

            Thread listenerThread = new Thread(() -> {
                try {
                    while (true) {
                        DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                        clientSocket.receive(receivePacket);
                        String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());

                        if (receivedMessage.contains("|SEQ:")) {
                            String[] parts = receivedMessage.split("\\|SEQ:");
                            String message = parts[0];
                            String ackMessage = "ACK " + parts[1];

                            InetAddress senderAddress = receivePacket.getAddress();
                            int senderPort = receivePacket.getPort();
                            DatagramPacket ackPacket = new DatagramPacket(ackMessage.getBytes(), ackMessage.length(),
                                    senderAddress, senderPort);
                            clientSocket.send(ackPacket);

                            System.out.println("\n" + message);
                        } else {
                            System.out.println("\n" + receivedMessage);
                        }
                        System.out.print("You: ");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            listenerThread.start();

            while (true) {
                System.out.print("You: ");
                String message = scanner.nextLine();
                byte[] messageData = message.getBytes();
                DatagramPacket sendPacket = new DatagramPacket(messageData, messageData.length, serverAddress, SERVER_PORT);
                clientSocket.send(sendPacket);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
