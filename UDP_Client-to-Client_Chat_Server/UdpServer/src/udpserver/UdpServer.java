package udpserver;

import java.net.*;
import java.util.*;

public class UdpServer {
    private static final int SERVER_PORT = 5001;
    private static final int BUFFER_SIZE = 1024;
    
    // Stores active clients with their IP & Port
    private static final Map<String, ClientInfo> clients = new HashMap<>();

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
            System.out.println("UDP Chat Server started on port " + SERVER_PORT);

            byte[] receiveBuffer = new byte[BUFFER_SIZE];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                // Extract client info
                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String clientKey = clientAddress.getHostAddress() + ":" + clientPort;
                
                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());

                // Register client if new
                if (!clients.containsKey(clientKey)) {
                    clients.put(clientKey, new ClientInfo(clientAddress, clientPort));
                    System.out.println("New client joined: " + clientKey);
                    broadcast(serverSocket, "New client joined: " + clientKey, clientKey);
                }

                System.out.println(clientKey + " says: " + receivedMessage);

                // Broadcast message to all other clients
                broadcast(serverSocket, clientKey + ": " + receivedMessage, clientKey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Broadcasts a message to all clients except the sender.
     */
    private static void broadcast(DatagramSocket serverSocket, String message, String senderKey) {
        byte[] sendData = message.getBytes();
        
        synchronized (clients) { // Prevents concurrency issues
            Iterator<Map.Entry<String, ClientInfo>> iterator = clients.entrySet().iterator();
            
            while (iterator.hasNext()) {
                Map.Entry<String, ClientInfo> entry = iterator.next();
                if (!entry.getKey().equals(senderKey)) {
                    try {
                        ClientInfo client = entry.getValue();
                        DatagramPacket sendPacket = new DatagramPacket(
                                sendData, sendData.length, client.address, client.port);
                        serverSocket.send(sendPacket);
                    } catch (Exception e) {
                        System.out.println("Client " + entry.getKey() + " disconnected.");
                        iterator.remove(); // Remove disconnected clients
                    }
                }
            }
        }
    }

    /**
     * Helper class to store client information.
     */
    private static class ClientInfo {
        InetAddress address;
        int port;

        public ClientInfo(InetAddress address, int port) {
            this.address = address;
            this.port = port;
        }
    }
}
