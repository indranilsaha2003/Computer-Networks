package whiteboardserver;

import java.io.*;
import java.net.*;
import java.util.*;

public class WhiteboardServer {
    private static final int PORT = 5555;
    private static final List<Socket> clients = Collections.synchronizedList(new ArrayList<>());
    private static final List<String> history = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server started on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                clients.add(clientSocket);
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class ClientHandler extends Thread {
        private Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
            ) {
                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Received from client: " + message);
                    
                    if (message.equals("SYNC")) {
                        // Send all previous canvas history
                        System.out.println("Sending sync data. History size: " + history.size());
                        synchronized (history) {
                            for (String line : history) {
                                out.println(line);
                            }
                        }
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
                        } else {
                            // Single stroke segment - we don't need to add this to history
                            // as it will be part of a combined stroke later
                        }
                        
                        // Broadcast to all except sender
                        broadcast(message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Client disconnected: " + socket.getInetAddress());
            } finally {
                clients.remove(socket);
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        // Method to broadcast to all clients EXCEPT the sender
        private void broadcast(String message) {
            synchronized (clients) {
                for (Socket client : clients) {
                    if (client != socket) {  // Skip the sender
                        try {
                            PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                            out.println(message);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        // Method to broadcast to ALL clients INCLUDING the sender
        private void broadcastToAll(String message) {
            System.out.println("Broadcasting to all clients: " + message);
            synchronized (clients) {
                for (Socket client : clients) {
                    try {
                        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                        out.println(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
