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
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.ReadPreference;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

/**
 * A chat client, that corresponds to a single user in the MongoChat system. It is connected to a
 * {@literal mongoDB} instance or replication set via a {@link #mongoClient}. It can be plugged into
 * any user interface by using its {@link #inputScanner} and {@link #outputWriter}.
 * 
 * <p>
 * It is expected to be used in multithreading environments and autostarts on creation.
 * 
 * @author Manuel Batsching
 *
 */
public class Client implements Runnable {

  /**
   * Represents the number of actively displayed messages.
   */
  private static final long MAX_COLLECTION_LENGTH = 20;

  /**
   * Name under which system messages are posted.
   */
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

  /**
   * @param socket {@link Socket} to derive input- and output-stream for user interaction
   * @see Socket#getInputStream
   * @see Socket#getOutputStream
   */
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

  /**
   * This method is automatically called on creation, so there should be no need to run it manually.
   */
  public void run() {

    // Wait for user input in while loop.
    while (!thread.isInterrupted()) {
      Scanner scanner = inputScanner;
      if (scanner != null && scanner.hasNextLine()) {

        String input = scanner.nextLine();
        // Check if user input is a chat command, like "!<command> [<parameter>]".
        Matcher commandMatcher =
            Pattern.compile("^!(?<command>[a-z]+) ?(?<parameter>.*)").matcher(input);

        if (commandMatcher.find()) {
          // "!quit" makes the client leave the chat.
          if (commandMatcher.group("command").equals("quit")) {
            sendSystemMessage(username + " left the Chat");
            break;
          }
          // "!name newName" renames the client.
          if (commandMatcher.group("command").equals("name")
              && !commandMatcher.group("parameter").isEmpty()) {
            String formerName = username;
            username = commandMatcher.group("parameter");
            sendSystemMessage(formerName + " is now called " + username);
            continue;
          }
          /*
           * !archive <hours> [<messageLimit>]" gives all messages from the message archive that
           * were send during the last number of hours, provided as parameter.
           * 
           * The optional second parameter limits the number of displayed messages.
           */
          if (commandMatcher.group("command").equals("archive")
              && !commandMatcher.group("parameter").isEmpty()) {
            BasicDBObject query = new BasicDBObject();
            // Only accept given parameters, if they are proper integers.
            Matcher parameterMatcher =
                Pattern.compile("^(?<hours>[0-9]+) ?(?<limit>[0-9]*)").matcher(
                    commandMatcher.group("parameter"));

            if (parameterMatcher.find()) {
              ChatDate date = new ChatDate();
              date.minusHours(Integer.parseInt(parameterMatcher.group("hours")));
              query.put("date", new BasicDBObject("$gte", date.toString()));
              FindIterable<Document> result = messageArchive.find(query);

              if (!parameterMatcher.group("limit").isEmpty()) {
                result.limit(Integer.parseInt(parameterMatcher.group("limit")));
              }
              
              outputWriter.println("### ARCHIVE START ###");
              for (Document messageDocument : result) {
                outputWriter.println(new Message(messageDocument));
              }
              outputWriter.println("### ARCHIVE END ###");
              continue;
            }
          }
        }
        sendMessage(new Message(input, username));
      }
    }
    // Do the clean up, after the thread was interrupted.
    chatObserver.interrupt();
    inputScanner.close();
    outputWriter.close();
    getMongoClient().close();
    try {
      socket.close();
    } catch (IOException e) {
      log.warn("Failed to close the socket, that was opened for the client.");
    }
  }

  /**
   * Send the message to the {@literal MessageCollection} of the chat database. This message will be
   * persistent and visible to all {@link Client}s, that follow the same
   * {@literal MessageCollection}.
   * 
   * @param message {@link Message}, that represents the message to be send
   */
  public void sendMessage(Message message) {
    // Archive messages, that would otherwise be silently overwritten in a capped collection.
    if (getMessageCollection().count() == MAX_COLLECTION_LENGTH) {
      messageArchive.insertOne(getMessageCollection().find().first());
    }
    getMessageCollection().insertOne(message.toDocument());
  }

  /**
   * A extension of {@link #sendMessage(Message)}, that allows a {@literal systemMessage} to be send
   * directly to the {@literal MessageCollection} of the chat database.
   * 
   * @param systemMessage a {@link String}, that contains the message to be send
   * @see #sendMessage(Message)
   */
  public void sendSystemMessage(String systemMessage) {
    log.info(systemMessage);
    sendMessage(new Message(systemMessage, MONGO_NAME));
  }

  /**
   * Sends the message directly to the {@literal outputWriter} of the current {@link Client}. The
   * message is not persistent and only visible to the current {@link Client}.
   * 
   * @param message {@link Message}, that represents the message to be send
   */
  public void receiveMessage(Message message) {
    outputWriter.println(message);
  }

  /**
   * A extension of {@link #receiveMessage(Message)}, that allows a {@literal systemMessage} to be
   * send directly to the users {@literal outputWriter}.
   * 
   * @param systemMessage a {@link String}, that contains the message to be send
   * @see #receiveMessage(Message)
   */
  public void receiveSystemMessage(String systemMessage) {
    log.info(systemMessage);
    receiveMessage(new Message(systemMessage, MONGO_NAME));
  }

  /**
   * Returns the Collection of active messages (the last 20).
   * 
   * @return {@link MongoCollection} of {@link Document}
   */
  public MongoCollection<Document> getMessageCollection() {
    return messageCollection;
  }

  /**
   * Returns the {@link MongoClient} that the current {@link Client} uses, to connect to the
   * {@literal mongoDB} instance or replication set.
   * 
   * @return {@link MongoClient}
   */
  public MongoClient getMongoClient() {
    return mongoClient;
  }

  /**
   * Reads the configuration data, that is needed to connect to a {@literal mongoDB} relplication
   * set from a user file in JSON format.
   * 
   * @param configFilePath a String that provides the path of the config file for the replication
   *        set relative to the Maven resource path.
   * @return {@link List} of {@link ServerAddress}
   */
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
      log.error("Cannot read replica set from config file. Do you have write access?");
      e.printStackTrace();
    }

    Type serverAddressListType = new TypeToken<List<ServerAddress>>() {}.getType();
    return new Gson().fromJson(jsonString, serverAddressListType);
  }
}
