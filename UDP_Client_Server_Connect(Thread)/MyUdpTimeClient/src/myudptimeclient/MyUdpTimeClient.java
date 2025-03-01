package myudptimeclient;

import java.net.*;
import java.io.*;

public class MyUdpTimeClient {
    public static void main(String[] args) {
        System.out.println("Server Time >>>>");

        try {
            DatagramSocket cs = new DatagramSocket();
            InetAddress ip = InetAddress.getByName("localhost");
            int port = 1234;

            byte[] rd = new byte[100];
            byte[] sd = new byte[100];

            DatagramPacket sp = new DatagramPacket(sd, sd.length, ip, port);
            DatagramPacket rp = new DatagramPacket(rd, rd.length);

            int timeout = 5000;
            cs.setSoTimeout(timeout);

            Thread writeThread = new Thread(() -> {
                try {
                    System.out.println("Sending request to the server...");
                    cs.send(sp);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });

            Thread readThread = new Thread(() -> {
                try {
                    System.out.println("Waiting for response from the server...");
                    cs.receive(rp);
                    String time = new String(rp.getData(), 0, rp.getLength());
                    System.out.println("Received time from server: " + time);
                } catch (SocketTimeoutException e) {
                    System.out.println("Timeout! No response from server.");
                } catch (IOException e) {
                    System.out.println("Error receiving response. Exiting...");
                } finally {
                    cs.close();
                }
            });

            writeThread.start();
            readThread.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
