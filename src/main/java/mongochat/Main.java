package mongochat;

import java.io.IOException;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  static Logger log;

  public static void main(String[] args) throws IOException {

    log = LoggerFactory.getLogger(Main.class);

    // Open a ServerSocket so that we can abuse telnet as a chat client user interface.
    ServerSocket serverSocket = new ServerSocket(4321);
    while (!Thread.currentThread().isInterrupted()) {
      try {
        // Create a new Client for each accepted connection on the ServerSocket.
        new Client(serverSocket.accept());

      } catch (IOException e) {
        log.error("Failed to establish a connection to the server socket.");
        e.printStackTrace();
      }
    }
    serverSocket.close();
    log.info("Server closed");
  }
}
