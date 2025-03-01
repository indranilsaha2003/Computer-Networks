package server;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class Server {
    private static final int MAX_CLIENTS = 10;
    private static final ClientHandler[] clients = new ClientHandler[MAX_CLIENTS];
    private static boolean serverRunning = true;
    private static final Scanner scanner = new Scanner(System.in);

    public static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(1555)) {
            System.out.println("Server started. Waiting for clients...");
            new Thread(Server::displayMenu).start();

            while (serverRunning) {
                Socket clientSocket = serverSocket.accept();
                int clientId = addClient(clientSocket);
                if (clientId == -1) {
                    System.out.println("Server is full.");
                    clientSocket.close();
                    continue;
                }
                System.out.println("New client connected with ID: " + clientId);
                clients[clientId] = new ClientHandler(clientSocket, clientId);
                new Thread(clients[clientId]).start();
            }
        } catch (IOException e) {
            System.out.println("Server shutting down...");
        }
    }

    private static synchronized int addClient(Socket socket) {
        for (int i = 0; i < MAX_CLIENTS; i++) {
            if (clients[i] == null) {
                return i;
            }
        }
        return -1;
    }

    private static synchronized void removeClient(int clientId) {
        clients[clientId] = null;
        System.out.println("Client " + clientId + " disconnected.");
    }

    private static void displayMenu() {
        while (serverRunning) {
            System.out.println("\nChoose an option:");
            System.out.println("1. Display total number of clients");
            System.out.println("2. Send a message to all clients");
            System.out.println("3. Send a message to a specific client");
            System.out.println("4. Exit (press 'q')");
            System.out.print("> ");
            String option = scanner.nextLine().trim();

            switch (option) {
                case "1":
                    int count = 0;
                    for (ClientHandler client : clients) {
                        if (client != null) count++;
                    }
                    System.out.println("Total connected clients: " + count);
                    break;
                case "2":
                    System.out.print("Enter message: ");
                    String message = scanner.nextLine();
                    broadcastMessage("Server: " + message);
                    break;
                case "3":
                    System.out.print("Enter client ID: ");
                    try {
                        int targetId = Integer.parseInt(scanner.nextLine().trim());
                        if (targetId >= 0 && targetId < MAX_CLIENTS && clients[targetId] != null) {
                            System.out.print("Enter message: ");
                            String msg = scanner.nextLine();
                            clients[targetId].sendMessage("Server (private): " + msg);
                        } else {
                            System.out.println("Invalid client ID.");
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid input. Enter a valid client ID.");
                    }
                    break;
                case "4":
                case "q":
                    System.out.println("Shutting down server...");
                    serverRunning = false;
                    for (int i = 0; i < MAX_CLIENTS; i++) {
                        if (clients[i] != null) {
                            clients[i].closeConnection();
                        }
                    }
                    System.exit(0);
                    break;
                default:
                    System.out.println("Invalid option. Try again.");
            }
        }
    }

    private static void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            if (client != null) {
                client.sendMessage(message);
            }
        }
    }

    static class ClientHandler implements Runnable {
        private final Socket socket;
        private final int clientId;
        private BufferedReader input;
        private PrintWriter output;

        public ClientHandler(Socket socket, int clientId) {
            this.socket = socket;
            this.clientId = clientId;
        }

        @Override
        public void run() {
            try {
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                output = new PrintWriter(socket.getOutputStream(), true);
                output.println("Your client ID is: " + clientId);

                while (true) {
                    String message = input.readLine();
                    if (message == null || message.equalsIgnoreCase("bye")) {
                        closeConnection();
                        break;
                    }

                    System.out.println("Client " + clientId + ": " + message);
                }
            } catch (IOException e) {
                System.out.println("Client " + clientId + " disconnected.");
            }
        }

        public void sendMessage(String message) {
            output.println(message);
        }

        public void closeConnection() {
            try {
                input.close();
                output.close();
                socket.close();
            } catch (IOException e) {
                System.out.println("Error closing connection.");
            }
            removeClient(clientId);
        }
    }
}
