package udpserver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class UdpServer {
    private static final int SERVER_PORT = 5001;
    private static final Map<String, InetAddress> clients = new HashMap<>();
    private static final Map<String, Integer> clientPorts = new HashMap<>();
    private static final Map<Integer, String> messageHistory = new HashMap<>();

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(SERVER_PORT)) {
            System.out.println("UDP Chat Server started on port " + SERVER_PORT);
            byte[] receiveBuffer = new byte[1024];

            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                serverSocket.receive(receivePacket);

                InetAddress clientAddress = receivePacket.getAddress();
                int clientPort = receivePacket.getPort();
                String clientKey = clientAddress.toString() + ":" + clientPort;
                String receivedMessage = new String(receivePacket.getData(), 0, receivePacket.getLength());

                if (!clients.containsKey(clientKey)) {
                    clients.put(clientKey, clientAddress);
                    clientPorts.put(clientKey, clientPort);
                    System.out.println("New client joined: " + clientKey);
                }

                if (receivedMessage.startsWith("ACK")) {
                    System.out.println("ACK received from " + clientKey);
                } else {
                    System.out.println(clientKey + " says: " + receivedMessage);
                    messageHistory.put(receivedMessage.hashCode(), receivedMessage);

                    for (String key : clients.keySet()) {
                        if (!key.equals(clientKey)) {
                            InetAddress targetAddress = clients.get(key);
                            int targetPort = clientPorts.get(key);
                            String msgWithAck = receivedMessage + "|SEQ:" + receivedMessage.hashCode();
                            byte[] sendData = msgWithAck.getBytes();
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                                    targetAddress, targetPort);
                            serverSocket.send(sendPacket);
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
