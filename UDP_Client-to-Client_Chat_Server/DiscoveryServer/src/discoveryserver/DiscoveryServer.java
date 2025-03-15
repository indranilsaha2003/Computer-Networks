package discoveryserver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.HashMap;
import java.util.Map;

public class DiscoveryServer {
    private static final int DISCOVERY_PORT = 9999;
    private static final Map<String, String> clients = new HashMap<>();

    public static void main(String[] args) {
        try (DatagramSocket serverSocket = new DatagramSocket(DISCOVERY_PORT)) {
            System.out.println("Discovery Server running on port " + DISCOVERY_PORT);

            byte[] buffer = new byte[1024];

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                serverSocket.receive(packet);
                String message = new String(packet.getData(), 0, packet.getLength());

                String clientKey = packet.getAddress().toString() + ":" + packet.getPort();

                if (message.startsWith("REGISTER")) {
                    clients.put(clientKey, message.substring(9)); // Register client
                    System.out.println("Registered: " + clientKey);
                } else if (message.startsWith("REQUEST")) {
                    StringBuilder response = new StringBuilder();
                    for (String client : clients.keySet()) {
                        if (!client.equals(clientKey)) {
                            response.append(client).append(";");
                        }
                    }
                    byte[] responseData = response.toString().getBytes();
                    DatagramPacket responsePacket = new DatagramPacket(responseData, responseData.length,
                            packet.getAddress(), packet.getPort());
                    serverSocket.send(responsePacket);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
