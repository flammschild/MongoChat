package mongochat;

import java.io.IOException;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  static Logger log;

  public static void main(String[] args) {

    log = LoggerFactory.getLogger(Main.class);

    // Open a ServerSocket, so that we can abuse telnet as a chat client user interface.
    ServerSocket serverSocket = null;
    try {
      serverSocket = new ServerSocket(4321);
      log.info("ServerSocket opened on 4321");
      
    } catch (IOException e) {
      log.warn("Failed to open a ServerSocket on 4321.");
      e.printStackTrace();
      // Kill the thread and end the program.
      Thread.currentThread().interrupt();
    }
    while (!Thread.currentThread().isInterrupted()) {
      try {
        // Create a new Client for each accepted connection on the ServerSocket.
        new Client(serverSocket.accept());

      } catch (IOException e) {
        log.warn("Failed to establish a connection to the server socket.");
        e.printStackTrace();
      }
    }
    try {
      serverSocket.close();
      log.info("Server closed");
      
    } catch (IOException e) {
      log.warn("Failed to close ServerSocket.");
      e.printStackTrace();
    }
  }
}
