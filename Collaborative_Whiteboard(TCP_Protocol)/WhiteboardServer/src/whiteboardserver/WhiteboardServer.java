package whiteboardserver;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class WhiteboardServer {
    private static final int PORT = 5555;
    // Using CopyOnWriteArrayList to avoid ConcurrentModificationException
    private static final List<Socket> clients = new CopyOnWriteArrayList<>();
    private static final List<String> history = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());
                    clients.add(clientSocket);
                    new ClientHandler(clientSocket).start();
                } catch (IOException e) {
                    System.err.println("Error accepting client connection: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Server failed to start: " + e.getMessage());
        }
    }

    static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private BufferedReader in;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received from client: " + message);
                    
                    if (message.equals("SYNC")) {
                        // Send all previous canvas history
                        System.out.println("Sending sync data. History size: " + history.size());
                        // Send the SYNC_START command to tell client to prepare
                        out.println("SYNC_START");
                        
                        synchronized (history) {
                            for (String line : history) {
                                out.println(line);
                            }
                        }
                        // Send the SYNC_END command to tell client sync is complete
                        out.println("SYNC_END");
                    } else if (message.equals("UNDO")) {
                        System.out.println("Processing UNDO command");
                        
                        // Remove the last stroke from server history first
                        synchronized (history) {
                            if (!history.isEmpty()) {
                                history.remove(history.size() - 1);
                                System.out.println("Removed last stroke from history. New size: " + history.size());
                            } else {
                                System.out.println("History is empty, nothing to undo");
                            }
                        }
                        
                        // Then broadcast the UNDO to ALL clients
                        broadcastToAll(message);
                    } else if (message.equals("REDO")) {
                        System.out.println("Processing REDO command");
                        
                        // Just broadcast the REDO to ALL clients
                        // Each client will handle its own redo stack
                        broadcastToAll(message);
                    } else if (message.equals("CLEAR")) {
                        System.out.println("Processing CLEAR command");
                        // Clear history first
                        synchronized (history) {
                            history.clear();
                            System.out.println("History cleared");
                        }
                        // Then broadcast
                        broadcastToAll(message);
                    } else {
                        // Regular drawing operation - this is from stroke segments
                        if (message.contains("|")) {
                            // This is a combined stroke with multiple segments
                            synchronized (history) {
                                history.add(message);
                            }
                        }
                        
                        // Broadcast to all except sender
                        broadcast(message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + socket.getInetAddress());
            } finally {
                cleanup();
            }
        }

        private void cleanup() {
            clients.remove(socket);
            
            try {
                if (in != null) in.close();
                if (out != null) out.close();
                socket.close();
            } catch (IOException e) {
                System.err.println("Error closing client resources: " + e.getMessage());
            }
        }

        // Method to broadcast to all clients EXCEPT the sender
        private void broadcast(String message) {
            for (Socket client : clients) {
                if (client != socket && !client.isClosed()) {  // Skip the sender and closed sockets
                    try {
                        PrintWriter clientOut = new PrintWriter(client.getOutputStream(), true);
                        clientOut.println(message);
                    } catch (IOException e) {
                        System.err.println("Error broadcasting to client: " + e.getMessage());
                        // Remove problematic client
                        clients.remove(client);
                        try {
                            client.close();
                        } catch (IOException closeErr) {
                            // Ignore close errors
                        }
                    }
                }
            }
        }

        // Method to broadcast to ALL clients INCLUDING the sender
        private void broadcastToAll(String message) {
            System.out.println("Broadcasting to all clients: " + message);
            for (Socket client : clients) {
                if (!client.isClosed()) {  // Skip closed sockets
                    try {
                        PrintWriter clientOut = new PrintWriter(client.getOutputStream(), true);
                        clientOut.println(message);
                    } catch (IOException e) {
                        System.err.println("Error broadcasting to client: " + e.getMessage());
                        // Remove problematic client
                        clients.remove(client);
                        try {
                            client.close();
                        } catch (IOException closeErr) {
                            // Ignore close errors
                        }
                    }
                }
            }
        }
    }
}