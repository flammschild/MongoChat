package mongochat;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.mongodb.CursorType;
import com.mongodb.MongoInterruptedException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

public class ChatObserverThread implements Runnable {

  private Client client;
  private Logger log;
  private Thread thread;
  private MongoCursor<Document> cursor;

  public ChatObserverThread(Client client) {
    this.client = client;
    log = LoggerFactory.getLogger(ChatObserverThread.class);
    thread = new Thread(this);
    thread.start();
  }

  public void run() {

    // Tail the MessageCollection for new messages and write them to the client's output.
    cursor = getTailableCursor(client.getMessageCollection());
    while (!thread.isInterrupted()) {
      try {
        Document nextMessage = cursor.tryNext();
        if (nextMessage != null) {
          // Use Json to convert a Document from the MessageCollection into a Message object.
          Message message = (new Gson()).fromJson(nextMessage.toJson(), Message.class);
          client.getOutputWriter().println(message);
        }
      } catch (Exception e) {
        client.getOutputWriter().println(
            "## We encountered a network issue: You may experience a slight delay. ##");
        log.warn("Message stream interrupted, trying to reload cursor because:" + e.getMessage());
        closeCursor();
        while (client.getMongoClient().getReplicaSetStatus().getMaster() == null) {
          log.info("No master present. Will retry in 2 sec.");
          try {
            Thread.sleep(2000);
          } catch (InterruptedException e1) {
            e1.printStackTrace();
          }
        }
        cursor = getTailableCursor(client.getMessageCollection());
        client.getOutputWriter().println("## Hooray! Network issue solved. ##");
      }
    }
    closeCursor();
  }

  private void closeCursor() {
    try {
      cursor.close();
    } catch (MongoSocketOpenException | MongoInterruptedException | IllegalStateException e) {
      // TODO: Ignore for now that socket cannot be reopened after cursor was closed and that
      // connection was interrupted. Should find way to close cursor more gracefully.
    }
  }

  public void interrupt() {
    thread.interrupt();
  }

  private MongoCursor<Document> getTailableCursor(MongoCollection<Document> collection) {
    // primaryPreferred provides a “read-only mode” during a failover.
    return collection.find().cursorType(CursorType.Tailable).iterator();
  }
}
