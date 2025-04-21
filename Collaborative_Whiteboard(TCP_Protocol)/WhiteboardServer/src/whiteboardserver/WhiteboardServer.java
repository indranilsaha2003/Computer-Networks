import java.io.*;
import java.net.*;
import java.util.*;

public class WhiteboardServer {
    private static final int PORT = 5000;
    private static final List<String> history = new ArrayList<>();
    private static final List<Socket> clients = Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = new ServerSocket(PORT);
        System.out.println("Whiteboard Server running on port " + PORT);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            System.out.println("New client connected: " + clientSocket);

            clients.add(clientSocket);
            new Thread(new ClientHandler(clientSocket)).start();
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            try {
                // Set socket read timeout: disconnect after 30 sec of inactivity
                socket.setSoTimeout(30000);
            } catch (SocketException e) {
                System.out.println("Failed to set timeout: " + e.getMessage());
            }
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

                // Send full history on initial connection
                synchronized (history) {
                    for (String stroke : history) {
                        out.println(stroke);
                    }
                }

                String line;
                while ((line = in.readLine()) != null) {
                    System.out.println("Received: " + line);

                    if (line.equals("SYNC")) {
                        // Send history again if SYNC is requested
                        synchronized (history) {
                            for (String stroke : history) {
                                out.println(stroke);
                            }
                        }
                    } else if (line.equals("CLEAR")) {
                        synchronized (history) {
                            history.clear();
                        }
                        broadcast("CLEAR", null);
                    } else if (line.equals("UNDO")) {
                        synchronized (history) {
                            if (!history.isEmpty()) {
                                history.remove(history.size() - 1);
                            }
                        }
                        broadcast("UNDO", null);
                    } else {
                        // Normal stroke
                        synchronized (history) {
                            history.add(line);
                            if (history.size() > 1000) {
                                history.remove(0); // limit memory usage
                            }
                        }
                        broadcast(line, socket); // send to all others
                    }
                }

            } catch (SocketTimeoutException e) {
                System.out.println("Client timed out: " + socket);
            } catch (IOException e) {
                System.out.println("Client error: " + e.getMessage());
            } finally {
                try {
                    socket.close();
                } catch (IOException e) {
                    // Ignore
                }
                clients.remove(socket);
                System.out.println("Client disconnected: " + socket);
            }
        }
    }

    private static void broadcast(String message, Socket excludeSocket) {
        synchronized (clients) {
            Iterator<Socket> iterator = clients.iterator();
            while (iterator.hasNext()) {
                Socket client = iterator.next();
                if (client != excludeSocket) {
                    try {
                        PrintWriter out = new PrintWriter(client.getOutputStream(), true);
                        out.println(message);
                    } catch (IOException e) {
                        System.out.println("Dead client detected. Removing: " + client);
                        try {
                            client.close();
                        } catch (IOException ex) {
                            // Ignore
                        }
                        iterator.remove();
                    }
                }
            }
        }
    }
}
