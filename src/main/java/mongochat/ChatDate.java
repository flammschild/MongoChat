package mongochat;

import java.util.Date;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

/**
 * A very simple wrapper class for {@link DateTime}, that provides input and output of chat dates
 * via a unified date pattern: {@literal "MMM dd, yyyy HH:mm:ss aaa"}.
 * 
 * @author Manuel Batsching
 * @see DateTimeFormat
 */
public class ChatDate {
  private static final String DATE_PATTERN = "MMM dd, yyyy HH:mm:ss aaa";
  private DateTime dateTime;

  /**
   * Constructs a {@link ChatDate}, which will use the date and time when the constructor was
   * called.
   */
  public ChatDate() {
    dateTime = DateTime.now();
  }

  /**
   * @param dateString a {@link String} that must correspond to the pattern
   *        {@literal "MMM dd, yyyy HH:mm:ss aaa"}
   * @see DateTimeFormat
   */
  public ChatDate(String dateString) {
    dateTime = DateTime.parse(dateString, DateTimeFormat.forPattern(DATE_PATTERN));
  }

  /**
   * @param date a {@link Date} object
   */
  public ChatDate(Date date) {
    dateTime = new DateTime(date);
  }

  /**
   * Substracts {@code hours} from the current date.
   * 
   * @param hours {@code int} value that gives the number of hours to be substracted.
   * @see DateTime#minusHours
   */
  public void minusHours(int hours) {
    dateTime.minusHours(hours);
  }

  /**
   * @return A {@link String} that corresponds to the pattern {@literal "d.M.yy hh:mm"}
   * @see DateTimeFormat
   */
  public String toShortString() {
    return dateTime.toString(DateTimeFormat.forPattern("d.M.yy hh:mm"));
  }

  public Date toDate() {
    return dateTime.toDate();
  }

  /**
   * @return A {@link String} that corresponds to the pattern {@literal "MMM dd, yyyy HH:mm:ss aaa"}
   * @see DateTimeFormat
   */
  @Override
  public String toString() {
    return dateTime.toString(DateTimeFormat.forPattern(DATE_PATTERN));
  }
}
