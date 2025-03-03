package myudptimeclient2;

import java.net.*;
import java.io.*;

public class MyUdpTimeClient2 {
    private static final int TIMEOUT = 5000; // 5 seconds timeout
    private static final int SLEEP_TIME = 5000; // 5 seconds sleep for write thread
    private static final String[] SERVER_LIST = {"localhost", "192.168.1.100", "192.168.1.101"}; // List of time servers
    private static final int DISCOVERY_PORT = 9999; // Known port for discovering the actual server port

    public static void main(String[] args) {
        System.out.println("Server Time >>>>");

        // Try each server in the list
        for (String server : SERVER_LIST) {
            Integer serverPort = discoverServerPort(server);
            if (serverPort != null) {
                System.out.println("Testing both timeout approaches for " + server);
                
                // Test Approach 1: Sleep timeout
                long start1 = System.currentTimeMillis();
                boolean success1 = tryServerMethod1(server, serverPort);
                long time1 = System.currentTimeMillis() - start1;
                
                // Test Approach 2: Socket timeout
                long start2 = System.currentTimeMillis();
                boolean success2 = tryServerMethod2(server, serverPort);
                long time2 = System.currentTimeMillis() - start2;
                
                System.out.println("Approach 1 (Sleep) took: " + time1 + "ms, success: " + success1);
                System.out.println("Approach 2 (Socket Timeout) took: " + time2 + "ms, success: " + success2);
                
                if (success1 || success2) {
                    System.out.println("Successfully connected to server: " + server);
                    return; // Stop once we get a successful response
                }
            }
        }

        System.out.println("All servers are unreachable. Exiting...");
    }

    // Method to discover the actual server port
    private static Integer discoverServerPort(String serverAddress) {
        try (DatagramSocket ds = new DatagramSocket()) {
            InetAddress ip = InetAddress.getByName(serverAddress);
            
            String request = "DISCOVER";
            byte[] sd = request.getBytes();
            DatagramPacket sp = new DatagramPacket(sd, sd.length, ip, DISCOVERY_PORT);
            
            byte[] rd = new byte[10]; // Buffer for receiving port number
            DatagramPacket rp = new DatagramPacket(rd, rd.length);

            ds.setSoTimeout(TIMEOUT);
            ds.send(sp);
            
            try {
                System.out.println("Trying a server...");
                ds.receive(rp);
                return Integer.parseInt(new String(rp.getData(), 0, rp.getLength()));
            } catch (SocketTimeoutException e) {
                System.out.println("Discovery timeout for server: " + serverAddress);
            }
        } catch (IOException e) {
            System.out.println("Discovery error with server: " + serverAddress);
        }
        return null; // Return null if discovery fails
    }

    // Approach 1: Using sleep in write thread
    private static boolean tryServerMethod1(String serverAddress, int port) {
        System.out.println("\nApproach 1: Using sleep timeout");
        final boolean[] responseReceived = new boolean[1];
        
        try {
            final DatagramSocket cs = new DatagramSocket();
            InetAddress ip = InetAddress.getByName(serverAddress);

            byte[] rd = new byte[100];
            byte[] sd = "TIME_REQUEST".getBytes();

            final DatagramPacket sp = new DatagramPacket(sd, sd.length, ip, port);
            final DatagramPacket rp = new DatagramPacket(rd, rd.length);

            // Write thread
            Thread writeThread = new Thread(() -> {
                try {
                    System.out.println("Sending request to server: " + serverAddress);
                    cs.send(sp);
                    Thread.sleep(SLEEP_TIME); // Sleep to cover timeout period
                } catch (Exception e) {
                    System.out.println("Write thread error: " + e.getMessage());
                }
            });

            // Read thread
            Thread readThread = new Thread(() -> {
                try {
                    System.out.println("Waiting for response from server: " + serverAddress);
                    cs.receive(rp);
                    responseReceived[0] = true;
                    String time = new String(rp.getData(), 0, rp.getLength());
                    System.out.println("Received time from server: " + time);
                } catch (IOException e) {
                    System.out.println("Read thread error: " + e.getMessage());
                }
            });

            writeThread.start();
            readThread.start();
            
            writeThread.join(); // Main thread waits for write thread to exit
            readThread.join(TIMEOUT); // Wait for read thread with timeout
            
            cs.close();
            return responseReceived[0];

        } catch (Exception e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            return false;
        }
    }

    // Approach 2: Using socket timeout
    private static boolean tryServerMethod2(String serverAddress, int port) {
        System.out.println("\nApproach 2: Using socket timeout");
        final boolean[] responseReceived = new boolean[1];
        
        try {
            final DatagramSocket cs = new DatagramSocket();
            InetAddress ip = InetAddress.getByName(serverAddress);

            byte[] rd = new byte[100];
            byte[] sd = "TIME_REQUEST".getBytes();

            final DatagramPacket sp = new DatagramPacket(sd, sd.length, ip, port);
            final DatagramPacket rp = new DatagramPacket(rd, rd.length);

            cs.setSoTimeout(TIMEOUT); // Set socket timeout

            // Write thread
            Thread writeThread = new Thread(() -> {
                try {
                    System.out.println("Sending request to server: " + serverAddress);
                    cs.send(sp);
                } catch (IOException e) {
                    System.out.println("Write thread error: " + e.getMessage());
                }
            });

            // Read thread
            Thread readThread = new Thread(() -> {
                try {
                    System.out.println("Waiting for response from server: " + serverAddress);
                    cs.receive(rp);
                    responseReceived[0] = true;
                    String time = new String(rp.getData(), 0, rp.getLength());
                    System.out.println("Received time from server: " + time);
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout! No response from server: " + serverAddress);
                } catch (IOException e) {
                    System.out.println("Read thread error: " + e.getMessage());
                }
            });

            writeThread.start();
            readThread.start();
            
            writeThread.join(); // Main thread waits for write thread to exit
            readThread.join(TIMEOUT + 1000); // Wait for read thread with timeout
            
            cs.close();
            return responseReceived[0];

        } catch (Exception e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            return false;
        }
    }
}