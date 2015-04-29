package mongochat;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.CursorType;
import com.mongodb.MongoClient;
import com.mongodb.MongoInterruptedException;
import com.mongodb.MongoSocketException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

/**
 * A Thread that uses the {@link MongoClient} of a {@link Client} to observe changes in the
 * MessageCollection and direct all new Messages to the {@link Client}s output stream.
 * 
 * <p>
 * It is expected to be used in multithreading environments and autostarts on creation.
 * 
 * @author Manuel Batsching
 *
 */
public class ChatObserverThread implements Runnable {

  private Client client;
  private Logger log;
  private Thread thread;
  private MongoCursor<Document> cursor;

  /**
   * Constructs a {@link ChatObserverThread} from a {@link Client}.
   * 
   * @param client a {@link Client} object
   */
  public ChatObserverThread(Client client) {
    this.client = client;
    log = LoggerFactory.getLogger(ChatObserverThread.class);
    thread = new Thread(this);
    thread.start();
  }

  /**
   * This method is automatically called on creation, so there should be no need to run it manually.
   */
  public void run() {

    // Tail the MessageCollection for new messages and write them to the client's output stream.
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
        waitForMaster();
        cursor = getTailableCursor(client.getMessageCollection());
        client.receiveSystemMessage("Hooray! Network issue solved.");
      } catch (MongoInterruptedException | IllegalStateException e) {
        // Ignore.
      }
    }
    closeCursor();
  }

  /**
   * Interrupt the current {@link ChatObserverThread}.
   * 
   * @see Thread#interrupt()
   */
  public void interrupt() {
    thread.interrupt();
  }

  /**
   * During a replication set failover wait until a new Master (Primary) is elected.
   */
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

  /**
   * Open an {@link MongoCursor} of the type {@link CursorType#Tailable} on a given collection. This
   * works only with capped collections. It will not be checked, if the collection is capped or not.
   * 
   * @param collection a {@literal mongoDB} capped collection.
   * @return a tailable {@link MongoCursor}
   */
  private MongoCursor<Document> getTailableCursor(MongoCollection<Document> collection) {
    MongoCursor<Document> cursor = null;
    try {
      cursor = collection.find().cursorType(CursorType.Tailable).iterator();
    } catch (IllegalStateException e) {
      log.warn("Tried to get a cursor from an already closed collection");
    }
    return cursor;
  }

  /**
   * Close the cursor and swallow all network related exceptions, that might be caused by this.
   */
  private void closeCursor() {
    try {
      cursor.close();
    } catch (MongoSocketException | MongoInterruptedException | IllegalStateException e) {
      // TODO: Ignore for now that socket cannot be reopened after cursor was closed and that
      // connection was interrupted. Should find way to close cursor more gracefully.
    }
  }
}
