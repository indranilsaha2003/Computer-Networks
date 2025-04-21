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
                    if (message.equals("SYNC")) {
                        // Send all previous canvas history
                        synchronized (history) {
                            for (String line : history) {
                                out.println(line);
                            }
                        }
                    } else if (message.equals("UNDO")) {
                        // Broadcast the UNDO command but don't add it to history
                        broadcast(message);
                    } else if (message.equals("CLEAR")) {
                        // Handle CLEAR - broadcast and clear history
                        broadcast(message);
                        synchronized (history) {
                            history.clear();  // Clear the history on CLEAR command
                        }
                    } else {
                        // Regular drawing operation
                        broadcast(message);
                        synchronized (history) {
                            history.add(message);  // Add to history
                        }
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

        private void broadcast(String message) {
            synchronized (clients) {
                for (Socket client : clients) {
                    if (client != socket) {  // Don't send back to the sender
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
}