package mongochat;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.mongodb.CursorType;
import com.mongodb.MongoCursorNotFoundException;
import com.mongodb.MongoInterruptedException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoSocketReadException;
import com.mongodb.MongoTimeoutException;
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
      } catch (MongoSocketReadException e) {
        log.warn("Message stream interrupted, trying to reload cursor");
        cursor.close();
        cursor = getTailableCursor(client.getMessageCollection());
      } catch (MongoSocketOpenException e) {
        log.error("Failed to open socket for cursor");
        e.printStackTrace();
      } catch (MongoTimeoutException e) {
        log.error("There is no primary server in the replication set. Cannot connect.");
        e.printStackTrace();
      } catch (MongoCursorNotFoundException IllegalStateException) {
        // TODO Find a way to avoid this exception
        log.warn("Tried to read a cursor, that was already gone.");
      } catch (MongoInterruptedException e) {
        log.warn("Cursor was forcefully removed from the pool.");
      }
    }
    cursor.close();
  }

  public void interrupt() {
    cursor.close();
    thread.interrupt();
  }

  private MongoCursor<Document> getTailableCursor(MongoCollection<Document> collection) {
    return collection.find().cursorType(CursorType.Tailable).iterator();
  }
}
