package mongochat;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;

public class Main {

  public static void main(String[] args) throws IOException {

    Logger log = LoggerFactory.getLogger(Main.class);

    // Read configuration for mongo replica set from config file. 
    URI configFileUri = null;
    try {
      configFileUri = Main.class.getResource("/replica_config.json").toURI();
    } catch (URISyntaxException e) {
      log.error("URI of replica set config file is malformed.");
      e.printStackTrace();
    } catch (NullPointerException e) {
      log.error("Cannot create URI of replica set config file. Does it exist?");
    }
    String jsonString = String.join("", Files.readAllLines(Paths.get(configFileUri)));
    Type serverAddressListType = new TypeToken<List<ServerAddress>>(){}.getType();
    List<ServerAddress> mongoReplicaSet = new Gson().fromJson(jsonString, serverAddressListType);

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
