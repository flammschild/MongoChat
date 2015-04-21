package mongochat;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

public class Main {

  public static void main(String[] args) throws IOException {
       
    Logger log = LoggerFactory.getLogger(Main.class);

    List<ServerAddress> mongoReplicaSet = new ArrayList<ServerAddress>();
    mongoReplicaSet.add(new ServerAddress("192.168.200.131", 27017));
    mongoReplicaSet.add(new ServerAddress("192.168.200.131", 27018));
    mongoReplicaSet.add(new ServerAddress("192.168.200.131", 27019));

    // Open a ServerSocket so that we can abuse telnet as a chat client user interface.
    ServerSocket serverSocket = new ServerSocket(4321);
    while (!Thread.currentThread().isInterrupted()) {
      try {
        // Create a new Client for each accepted connection on the ServerSocket.
        Client client = new Client(serverSocket.accept());
        client.setMongoClient(new MongoClient(mongoReplicaSet));
        client.setMongoDatabase("test");
        client.setMessageCollection("messages");
        client.setMessageArchive("messagearchive");
        client.setChatObserver(new ChatObserverThread(client));
        log.info("{} joined the chat", client.getUsername());

      } catch (IOException e) {
        log.error("Failed to establish a connection to the server socket.");
        e.printStackTrace();
      }
    }
    serverSocket.close();
    log.info("Server closed");
  }
}
