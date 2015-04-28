package mongochat;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.CursorType;
import com.mongodb.MongoInterruptedException;
import com.mongodb.MongoSocketException;
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
          client.receiveMessage(new Message(nextMessage));
        }
      } catch (MongoSocketException e) {
        client
            .receiveSystemMessage("A wild network issue appeared! You may experience a slight delay.");
        log.warn("Message stream interrupted. Trying to reload cursor.");
        closeCursor();
        waitForMaster(); // During a failover wait until a new Master (Primary) is elected.
        cursor = getTailableCursor(client.getMessageCollection());
        client.receiveSystemMessage("Hooray! Network issue solved.");
      } catch (MongoInterruptedException e){
        // Ignore.
      }
    }
    closeCursor();
  }

  public void interrupt() {
    thread.interrupt();
  }

  private void waitForMaster() {
    try {
      while (client.getMongoClient().getReplicaSetStatus().getMaster() == null) {
        log.info("No master present. Will retry in 2 sec.");
        Thread.sleep(2000);
      }
    } catch (InterruptedException | IllegalStateException e) {
      // Silently continue, if connection is closed or sleep is interrupted.
    }
  }

  private MongoCursor<Document> getTailableCursor(MongoCollection<Document> collection) {
    MongoCursor<Document> cursor = null;
    try {
      cursor = collection.find().cursorType(CursorType.Tailable).iterator();
    } catch (IllegalStateException e) {
      log.info("Tried to get a cursor from an already closed collection");
    }
    return cursor;
  }

  private void closeCursor() {
    try {
      cursor.close();
    } catch (MongoSocketException | MongoInterruptedException | IllegalStateException e) {
      // TODO: Ignore for now that socket cannot be reopened after cursor was closed and that
      // connection was interrupted. Should find way to close cursor more gracefully.
    }
  }
}
