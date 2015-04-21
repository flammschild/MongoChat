package mongochat;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import com.google.gson.Gson;

public class Message {

  private Date date;
  private String text;
  private String userName;

  public Message(String text, String userName, Date date) {
    this.text = text;
    this.date = date;
    this.userName = userName;
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

  public void setDate(Date date) {
    this.date = date;
  }

  public void setText(String text) {
    this.text = text;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  public String toJson() {
    return (new Gson()).toJson(this);
  }

  @Override
  public String toString() {
    // Format a Message in a way that can be displayed to the user.
    DateFormat df =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, Locale.GERMAN);
    return String.format("%s (%s): %s", getUserName(), df.format(getDate()), getText());
  }
}
