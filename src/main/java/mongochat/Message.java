package mongochat;

import java.util.Date;

import org.bson.Document;

import com.google.gson.Gson;

public class Message {

  private Date date;
  private String text;
  private String userName;

  public Message(String text, String userName) {
    this.text = text;
    this.date = new ChatDate().toDate();
    this.userName = userName;
  }

  public Message(String text, String userName, Date date) {
    this.text = text;
    this.date = date;
    this.userName = userName;
  }

  public Message(Document messageDocument) {
    text = messageDocument.getString("text");
    date = new ChatDate(messageDocument.getString("date")).toDate();
    userName = messageDocument.getString("userName");

  }

  public Date getDate() {
    return date;
  }

  public String getText() {
    return text;
  }

  public String getUserName() {
    return userName;
  }

  public String toJson() {
    return (new Gson()).toJson(this);
  }

  public Document toDocument() {
    return Document.parse(this.toJson());
  }

  @Override
  public String toString() {
    // Format a Message in a way that can be displayed to the user.
    return String.format("%s (%s): %s", getUserName(), (new ChatDate(getDate())).toShortString(),
        getText());
  }
}
