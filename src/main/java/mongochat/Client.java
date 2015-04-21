package mongochat;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Date;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

public class Client implements Runnable {

  private ChatObserverThread chatObserver;
  private Scanner inputScanner;
  private PrintWriter outputWriter;
  private MongoCollection<Document> messageCollection;
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
    setUsername(thread.getName());
    try {
      setInputScanner(new Scanner(socket.getInputStream()));
      setOutputWriter(new PrintWriter(socket.getOutputStream(), true));
    } catch (IOException e) {
      log.error("Something went wrong when setting input and output of the client.");
      e.printStackTrace();
    }
  }

  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      Scanner scanner = getInputScanner();
      if (scanner != null && scanner.hasNextLine()) {

        String input = scanner.nextLine();
        // Check if user input is a chat command, like "!<command> <parameter>".
        Matcher matcher = Pattern.compile("^!([a-z]+) ?(.*)").matcher(input);

        if (matcher.find()) {
          // "!quit" makes the client leave the chat.
          if (matcher.group(1).equals("quit")) {
            log.info("{} has left the Chat", getUsername());
            break;
          }
          // "!name newName" renames the client.
          if (matcher.group(1).equals("name") && !matcher.group(2).isEmpty()) {
            String formerName = getUsername();
            setUsername(matcher.group(2));
            log.info("{} is now called {}", formerName, getUsername());
            continue;
          }
        }
        sendMessage(new Message(input, getUsername(), new Date()));
      }
    }
    cleanUp();
  }

  public ChatObserverThread getChatObserver() {
    return this.chatObserver;
  }

  public Scanner getInputScanner() {
    return inputScanner;
  }

  public MongoCollection<Document> getMessageCollection() {
    return messageCollection;
  }

  public MongoClient getMongoClient() {
    return this.mongoClient;
  }

  public MongoDatabase getMongoDatabase() {
    return mongoDatabase;
  }

  public PrintWriter getOutputWriter() {
    return outputWriter;
  }

  public Socket getSocket() {
    return this.socket;
  }

  public String getUsername() {
    return this.username;
  }

  public void setChatObserver(ChatObserverThread chatObserverThread) {
    this.chatObserver = chatObserverThread;

  }

  public void setInputScanner(Scanner inputScanner) {
    this.inputScanner = inputScanner;
  }

  public void setMessageCollection(String collectionName) {
    messageCollection = getMongoDatabase().getCollection(collectionName);
  }

  public void setMongoClient(MongoClient mongoClient) {
    this.mongoClient = mongoClient;

  }

  public void setMongoDatabase(String databaseName) {
    this.mongoDatabase = getMongoClient().getDatabase(databaseName);
  }

  public void setOutputWriter(PrintWriter outputWriter) {
    this.outputWriter = outputWriter;
  }

  public void setUsername(String username) {
    this.username = username;

  }

  public void sendMessage(Message message) {
    Document document = Document.parse(message.toJson());
    getMessageCollection().insertOne(document);
    log.debug("Message sent: {}", document.toJson());
  }

  public void interrupt() {
    thread.interrupt();
  }

  public void cleanUp() {
    getChatObserver().interrupt();
    getInputScanner().close();
    getOutputWriter().close();
    getMongoClient().close();
    try {
      getSocket().close();
    } catch (IOException e) {
      log.warn("Failed to close the socket, that was opened for the client.");
      e.printStackTrace();
    }
    interrupt();
  }
}
