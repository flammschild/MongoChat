package mongochat;

import java.util.Date;

import org.bson.Document;

import com.google.gson.Gson;

/**
 * Represents a chat message, that is convenient to use with mongoDB collections.
 * 
 * @author Manuel Batsching
 *
 */
public class Message {

  private Date date;
  private String text;
  private String userName;

  /**
   * Constructs a {@link Message} from a message text and user name. The date is set to the point at
   * which the Message gets created.
   * 
   * @param text a {@link String} that represents the message text
   * @param userName a {@link String} that represents the user name
   */
  public Message(String text, String userName) {
    this.text = text;
    this.userName = userName;
    date = new ChatDate().toDate();

  }

  /**
   * Constructs a {@link Message} from a message text, userName and date.
   * 
   * @param text a {@link String} that represents the message text
   * @param userName a {@link String} that represents the user name
   * @param date a {@link Date} that represents the time that should be used with the message.
   */
  public Message(String text, String userName, Date date) {
    this.text = text;
    this.userName = userName;
    this.date = date;
  }

  /**
   * 
   * Constructs a {@link Message} from a BSON {@link Document}, that has the following structure:
   * 
   * <p>
   * {@code "date" : date, "text" : text, "userName" : userName}
   * 
   * @param messageDocument A {@link Document} from a {@literal mongoDB} Collection.
   */
  public Message(Document messageDocument) {
    text = messageDocument.getString("text");
    userName = messageDocument.getString("userName");
    date = new ChatDate(messageDocument.getString("date")).toDate();
  }

  /**
   * @return {@link Date}
   */
  public Date getDate() {
    return date;
  }

  /**
   * @return the message text as {@link String}
   */
  public String getText() {
    return text;
  }

  /**
   * @return the user name as {@link String}
   */
  public String getUserName() {
    return userName;
  }

  /**
   * @return a representation of the current {@link Message} object as JSON-{@link String}
   */
  public String toJson() {
    return (new Gson()).toJson(this);
  }

  /**
   * @return a representation of the current {@link Message} object as a {@link Document} from a
   *         {@literal mongoDB} Collection
   */
  public Document toDocument() {
    return Document.parse(this.toJson());
  }

  /**
   * Formats the current {@link Message} in a "chat-style", that can be conveniently displayed to
   * the user in the client output stream. For example:
   * 
   * <p>
   * "Foobert (29.4.15 04:08): The truth value of this sentence is false."
   * 
   * @return a representation of the current {@link Message} object as {@link String}
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return String.format("%s (%s): %s", getUserName(), (new ChatDate(getDate())).toShortString(),
        getText());
  }
}
