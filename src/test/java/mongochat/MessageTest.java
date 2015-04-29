package mongochat;

import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.regex.Pattern;

import org.junit.Test;

import com.google.gson.Gson;

public class MessageTest {

	@Test
	public void testGetDate() {
		Message testMessage = new Message("Hello World", "Testuser", new Date());
		
		assertTrue(testMessage.getDate() instanceof Date);
	}

	@Test
	public void testGetText() {		
		Message testMessage = new Message("Hello World", "Testuser", new Date());
		
		assertTrue(testMessage.getText().equals("Hello World"));
	}

	@Test
	public void testGetUserName() {
		Message testMessage = new Message("Hello World", "Testuser", new Date());
		
		assertTrue(testMessage.getUserName().equals("Testuser"));
	}

	@Test
	public void testMessageToString() {
		Message testMessage = new Message("Hello World", "Testuser", new Date());
	
		assertTrue(Pattern
				.matches(
						"Testuser \\([0-3]?[\\d]\\.[0-1]?[\\d]\\.[\\d]{2} [0-2]?[\\d]:[0-5]?[\\d]\\): Hello World",
						testMessage.toString()));
	}

	@Test
	public void testToJson() {
		Message testMessage = new Message("Hello World", "Testuser", new Date());
		String jsonString = testMessage.toJson();
		
		(new Gson()).fromJson(jsonString, Message.class);
	}
}
