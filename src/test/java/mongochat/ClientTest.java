package mongochat;

import static org.junit.Assert.assertTrue;

import java.util.Date;

import mongochat.ChatObserverThread;
import mongochat.Client;
import mongochat.Message;

import org.bson.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

/*import de.flapdoodle.embedmongo.MongoDBRuntime;
import de.flapdoodle.embedmongo.MongodExecutable;
import de.flapdoodle.embedmongo.MongodProcess;
import de.flapdoodle.embedmongo.config.MongodConfig;
import de.flapdoodle.embedmongo.distribution.Version;
import de.flapdoodle.embedmongo.runtime.Network;*/

public class ClientTest {

 /* private static final String DB_NAME = "chat_embedded";
  private MongodProcess mongod;
  private MongodExecutable mongodExe;
  private Client testClient;

  @Before
   public void setup() throws Exception {
  
   // Creating Mongodb runtime instance
   MongoDBRuntime runtime = MongoDBRuntime.getDefaultInstance();
  
   // Creating MongodbExecutable
   mongodExe = runtime.prepare(new MongodConfig(Version.V2_2_0_RC0, 12345,
   Network.localhostIsIPv6()));
  
   // Starting Mongodb
   mongod = mongodExe.start();
  
   // Create test Chat client
   testClient = new Client(null); //TODO Mock socket!
   testClient.setMongoClient(new MongoClient("127.0.0.1", 12345));
   testClient.setMongoDatabase(DB_NAME);
   testClient.setMessageCollection("messages");
   testClient.setChatObserver(new ChatObserverThread(testClient));

   }
  @After
  public void teardown() throws Exception {
    mongod.stop();
    mongodExe.cleanup();
  }

  @Test
  public void testSendMessage() {
    Message message = new Message("Hello World", testClient.getUsername(), new Date());
    testClient.sendMessage(message);

    BasicDBObject searchQuery = new BasicDBObject();
    searchQuery.put("text", "Hello world");
    MongoCursor<Document> cursor = testClient.getMessageCollection().find(searchQuery).iterator();
    assertTrue(cursor.hasNext());
    cursor.close();
  }

  @Test
  public void testSetMessageCollection() {
    assertTrue(testClient.getMessageCollection() instanceof MongoCollection<?>);
  }

  @Test
  public void testSetMongoConnection() {
    assertTrue(testClient.getMongoDatabase() instanceof MongoDatabase);
  }*/
}
