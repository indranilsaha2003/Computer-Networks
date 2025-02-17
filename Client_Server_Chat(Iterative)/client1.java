package client1;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class client1 {
  private static BufferedReader input;
  private static PrintWriter output;

  public static void main(String[] args) {
    try (Socket socket = new Socket("localhost", 1254);
        Scanner scanner = new Scanner(System.in)) {

      input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      output = new PrintWriter(socket.getOutputStream(), true);

      System.out.println(input.readLine()); // Read greeting message

      // Start a separate thread for listening to incoming messages
      Thread listenerThread = new Thread(Client1::listenForMessages);
      listenerThread.setDaemon(true);
      listenerThread.start();

      while (true) {
        printCommandMenu();
        System.out.print("Enter command: ");
        String command = scanner.nextLine();

        if (command.equalsIgnoreCase("EXIT")) {
          System.out.println("Disconnecting...");
          output.println(command);
          break;
        }

        output.println(command);

        // GET response should be immediate
        if (command.equalsIgnoreCase("GET")) {
          System.out.println("Server response: " + input.readLine());
        }
      }

    } catch (IOException e) {
      System.out.println("Error: " + e.getMessage());
    }
  }

  private static void listenForMessages() {
    try {
      String receivedMessage;
      while ((receivedMessage = input.readLine()) != null) {
        System.out.println("\n[NEW MESSAGE] " + receivedMessage);
        System.out.print("Enter command: ");
      }
    } catch (IOException e) {
      System.out.println("Disconnected from server.");
    }
  }

  private static void printCommandMenu() {
    System.out.println("1. GET - Fetch the list of connected clients.");
    System.out.println("2. SEND <id> <msg> - Send a message to a specific client.");
    System.out.println("3. EXIT - Disconnect from the server.");
  }
}
