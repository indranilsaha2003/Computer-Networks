package myudptimeserver;

import java.net.*;
import java.io.*;
import java.util.Date;

public class MyUdpTimeServer {
    public static void main(String[] args) {
        int port = 1234;

        try (DatagramSocket ss = new DatagramSocket(port)) {
            System.out.println("UDP Time Server is running...");

            while (true) {
                byte[] rd = new byte[100];
                DatagramPacket rp = new DatagramPacket(rd, rd.length);

                // Receive request
                ss.receive(rp);
                System.out.println("Received request from client.");

                // Get current time
                String time = new Date().toString();
                byte[] sd = time.getBytes();

                // Send response
                DatagramPacket sp = new DatagramPacket(sd, sd.length, rp.getAddress(), rp.getPort());
                ss.send(sp);
                System.out.println("Sent time to client: " + time);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
