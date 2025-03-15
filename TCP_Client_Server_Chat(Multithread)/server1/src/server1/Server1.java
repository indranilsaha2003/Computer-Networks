package server1;

import java.io.*;
import java.net.*;

public class Server1 {
  private static final int MAX_CLIENTS = 10;
  private static ClientHandler[] clients = new ClientHandler[MAX_CLIENTS];

  public static void main(String[] args) {
    try (ServerSocket serverSocket = new ServerSocket(1254)) {
      System.out.println("Server started. Waiting for clients...");

      while (true) {
        Socket socket = serverSocket.accept();
        int clientId = assignClientId();

        if (clientId == -1) {
          System.out.println("Server full! Connection rejected.");
          socket.close();
          continue;
        }

        ClientHandler clientHandler = new ClientHandler(socket, clientId);
        clients[clientId] = clientHandler;

        Thread clientThread = new Thread(clientHandler);
        clientThread.start();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static synchronized int assignClientId() {
    for (int i = 0; i < MAX_CLIENTS; i++) {
      if (clients[i] == null) {
        return i;
      }
    }
    return -1;
  }

  private static synchronized String getClientList() {
    StringBuilder clientList = new StringBuilder("Connected clients: ");
    for (int i = 0; i < MAX_CLIENTS; i++) {
      if (clients[i] != null) {
        clientList.append(i).append(" ");
      }
    }
    return clientList.toString();
  }

  public static synchronized void sendMessage(int fromClient, int toClient, String message) {
    if (toClient >= 0 && toClient < MAX_CLIENTS && clients[toClient] != null) {
      clients[toClient].sendMessage("Client " + fromClient + " says: " + message);
    } else {
      clients[fromClient].sendMessage("Client " + toClient + " is not connected.");
    }
  }

  public static synchronized void removeClient(int clientId) {
    if (clientId >= 0 && clientId < MAX_CLIENTS) {
      clients[clientId] = null;
      System.out.println("Client " + clientId + " disconnected.");
    }
  }

  private static class ClientHandler implements Runnable {
    private Socket socket;
    private int clientId;
    private BufferedReader input;
    private PrintWriter output;

    public ClientHandler(Socket socket, int clientId) {
      this.socket = socket;
      this.clientId = clientId;
    }

    public void sendMessage(String message) {
      output.println(message);
    }

    @Override
    public void run() {
      try {
        input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        output = new PrintWriter(socket.getOutputStream(), true);

        output.println("Connected as Client " + clientId);
        System.out.println("Client " + clientId + " connected.");

        while (true) {
          String message = input.readLine();
          if (message == null)
            break;

          if (message.equalsIgnoreCase("GET")) {
            output.println(getClientList());
          } else if (message.startsWith("SEND")) {
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) {
              output.println("Invalid command. Use: SEND <client_id> <message>");
            } else {
              try {
                int targetClient = Integer.parseInt(parts[1]);
                Server1.sendMessage(clientId, targetClient, parts[2]);
              } catch (NumberFormatException e) {
                output.println("Invalid client ID.");
              }
            }
          } else if (message.equalsIgnoreCase("EXIT")) {
            break;
          } else {
            output.println("Unknown command.");
          }
        }
      } catch (IOException e) {
        System.out.println("Client " + clientId + " disconnected unexpectedly.");
      } finally {
        removeClient(clientId);
        try {
          socket.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }
}