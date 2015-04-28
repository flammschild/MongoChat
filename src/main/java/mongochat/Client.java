package mongochat;

import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Type;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Client implements Runnable {

  // Represents the number of actively displayed messages.
  private static final long MAX_COLLECTION_LENGTH = 20;

  // Name under which system messages are posted.
  private static final String MONGO_NAME = "## MongoChat ##";

  private ChatObserverThread chatObserver;
  private Scanner inputScanner;
  private PrintWriter outputWriter;
  private MongoCollection<Document> messageCollection;
  private MongoCollection<Document> messageArchive;
  private MongoClient mongoClient;
  private MongoDatabase mongoDatabase;
  private Socket socket;
  private String username;
  private Logger log = LoggerFactory.getLogger(Client.class);
  private Thread thread;

  public Client(Socket socket) {
    this.socket = socket;
    thread = new Thread(this);
    thread.start();
    username = thread.getName();
    try {
      inputScanner = new Scanner(socket.getInputStream());
      outputWriter = new PrintWriter(socket.getOutputStream(), true);
    } catch (IOException e) {
      log.error("Something went wrong when setting input and output of the client.");
      e.printStackTrace();
    }

    // Read configuration for mongodb replica set from config file.
    MongoClient mongoClient = null;
    mongoClient = new MongoClient(getMongoReplicaSetServers("/replica_config.json"));
    mongoClient.setReadPreference(ReadPreference.primaryPreferred());
    mongoClient.setWriteConcern(WriteConcern.REPLICA_ACKNOWLEDGED);

    // Configure client to participate in chat.
    this.mongoClient = mongoClient;
    mongoDatabase = getMongoClient().getDatabase("test");
    messageCollection = mongoDatabase.getCollection("messages");
    messageArchive = mongoDatabase.getCollection("messagearchive");
    chatObserver = new ChatObserverThread(this);
    sendSystemMessage(username + " joined the Chat");
  }

  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      Scanner scanner = inputScanner;
      if (scanner != null && scanner.hasNextLine()) {

        String input = scanner.nextLine();
        // Check if user input is a chat command, like "!<command> <parameter>".
        Matcher matcher = Pattern.compile("^!([a-z]+) ?(.*)").matcher(input);

        if (matcher.find()) {
          // "!quit" makes the client leave the chat.
          if (matcher.group(1).equals("quit")) {
            sendSystemMessage(username + " left the Chat");
            break;
          }
          // "!name newName" renames the client.
          if (matcher.group(1).equals("name") && !matcher.group(2).isEmpty()) {
            String formerName = username;
            username = matcher.group(2);
            sendSystemMessage(formerName + " is now called " + username);
            continue;
          }
          // "!archive" shows all archived Messages
          if (matcher.group(1).equals("archive")) {
            outputWriter.println("### ARCHIVE START ###");
            for (Document messageDocument : messageArchive.find()) {
              outputWriter.println(new Message(messageDocument));
            }
            outputWriter.println("### ARCHIVE END ###");
            continue;
          }
        }
        sendMessage(new Message(input, username));
      }
    }
    // Clean up.
    chatObserver.interrupt();
    inputScanner.close();
    outputWriter.close();
    getMongoClient().close();
    try {
      socket.close();
    } catch (IOException e) {
      log.warn("Failed to close the socket, that was opened for the client.");
      e.printStackTrace();
    }
    thread.interrupt();
  }

  public void sendMessage(Message message) {
    // Archive messages, that would otherwise be silently overwritten in a capped collection.
    if (getMessageCollection().count() == MAX_COLLECTION_LENGTH) {
      messageArchive.insertOne(getMessageCollection().find().first());
    }
    getMessageCollection().insertOne(message.toDocument());
  }

  public void sendSystemMessage(String systemMessage) {
    log.info(systemMessage);
    getMessageCollection().insertOne(new Message(systemMessage, MONGO_NAME).toDocument());
  }

  public void receiveSystemMessage(String systemMessage) {
    log.info(systemMessage);
    receiveMessage(new Message(systemMessage, MONGO_NAME));
  }

  public MongoCollection<Document> getMessageCollection() {
    return messageCollection;
  }

  public MongoClient getMongoClient() {
    return mongoClient;
  }

  public void receiveMessage(Message message) {
    outputWriter.println(message);
  }

  private List<ServerAddress> getMongoReplicaSetServers(String configFilePath) {
    URI configFileUri = null;
    String jsonString = null;
    try {
      configFileUri = Main.class.getResource(configFilePath).toURI();
      jsonString = String.join("", Files.readAllLines(Paths.get(configFileUri)));
    } catch (URISyntaxException e) {
      log.error("URI of replica set config file is malformed.");
      e.printStackTrace();
    } catch (NullPointerException e) {
      log.error("Cannot create URI of replica set config file. Does it exist?");
    } catch (IOException e) {
      log.error("Cannot read replica set fron config file. Do you have write access?");
      e.printStackTrace();
    }

    Type serverAddressListType = new TypeToken<List<ServerAddress>>() {}.getType();
    return new Gson().fromJson(jsonString, serverAddressListType);
  }
}
