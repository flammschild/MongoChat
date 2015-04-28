package mongochat;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.bson.Document;

import com.google.gson.Gson;

public class Message {

  private Date date;
  private String text;
  private String userName;

  public Message(String text, String userName) {
    this.text = text;
    this.date = new Date();
    this.userName = userName;
  }

  public Message(String text, String userName, Date date) {
    this.text = text;
    this.date = date;
    this.userName = userName;
  }

  public Message(Document messageDocument) {
    text = messageDocument.getString("text");
    userName = messageDocument.getString("userName");
    SimpleDateFormat df = new SimpleDateFormat("MMM dd, yyyy HH:mm:ss aaa"); // Apr 28, 2015 11:52:17 PM
    try {
      date = df.parse(messageDocument.getString("date"));
    } catch (ParseException e) {
      e.printStackTrace();
    }
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
    DateFormat df =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.GERMAN);
    return String.format("%s (%s): %s", getUserName(), df.format(getDate()), getText());
  }
}
