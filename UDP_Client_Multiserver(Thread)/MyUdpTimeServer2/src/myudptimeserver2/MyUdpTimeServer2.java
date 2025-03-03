package myudptimeserver2;

import java.net.*;
import java.io.*;
import java.util.Date;

public class MyUdpTimeServer2 {
    private static final int DISCOVERY_PORT = 9999; // Known port for discovery

    public static void main(String[] args) {
        try {
            DatagramSocket discoverySocket = new DatagramSocket(DISCOVERY_PORT);
            DatagramSocket timeSocket = new DatagramSocket(0); // Bind to a random available port
            int assignedPort = timeSocket.getLocalPort();

            System.out.println("UDP Time Server is running on port " + assignedPort + "...");

            // Thread to handle discovery requests
            new Thread(() -> {
                try {
                    while (true) {
                        byte[] rd = new byte[100];
                        DatagramPacket rp = new DatagramPacket(rd, rd.length);
                        discoverySocket.receive(rp);

                        InetAddress clientAddress = rp.getAddress();
                        int clientPort = rp.getPort();
                        String request = new String(rp.getData(), 0, rp.getLength());

                        if (request.equals("DISCOVER")) {
                            byte[] sd = String.valueOf(assignedPort).getBytes();
                            DatagramPacket sp = new DatagramPacket(sd, sd.length, clientAddress, clientPort);
                            discoverySocket.send(sp);
                            System.out.println("Sent assigned port (" + assignedPort + ") to client: " + clientAddress);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

            // Main loop for handling time requests - concurrent server design
            new Thread(() -> {
                try {
                    while (true) {
                        byte[] rd = new byte[100];
                        DatagramPacket rp = new DatagramPacket(rd, rd.length);
                        timeSocket.receive(rp);
                        
                        // Handle each client request in a new thread
                        new Thread(() -> {
                            try {
                                InetAddress clientAddress = rp.getAddress();
                                int clientPort = rp.getPort();
                                System.out.println("Received request from client: " + clientAddress + ":" + clientPort);

                                String time = new Date().toString();
                                byte[] sd = time.getBytes();

                                DatagramPacket sp = new DatagramPacket(sd, sd.length, clientAddress, clientPort);
                                timeSocket.send(sp);
                                System.out.println("Sent time to client: " + time);
                            } catch (IOException e) {
                                System.out.println("Error handling client: " + e.getMessage());
                            }
                        }).start();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            
            // Keep main thread alive
            while (true) {
                Thread.sleep(10000);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}