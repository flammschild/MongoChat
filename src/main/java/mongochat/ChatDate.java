package mongochat;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class ChatDate {
  private static final String DATE_PATTERN = "MMM dd, yyyy HH:mm:ss aaa";
  private DateTime dateTime;

  public ChatDate() {
    dateTime = DateTime.now();
  }

  public ChatDate(String dateString) {
    dateTime = DateTime.parse(dateString, DateTimeFormat.forPattern(DATE_PATTERN));
  }

  public ChatDate(Date date) {
    dateTime = new DateTime(date);
  }

  public void minusHours(int hours) {
    dateTime.minusHours(hours);
  }

  public String toShortString() {
    return dateTime.toString(DateTimeFormat.forPattern("d.M.yy hh:mm"));
  }

  public Date toDate() {
    return dateTime.toDate();
  }

  @Override
  public String toString() {
    return dateTime.toString(DateTimeFormat.forPattern(DATE_PATTERN));
  }
}
