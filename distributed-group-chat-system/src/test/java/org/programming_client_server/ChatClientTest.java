package org.programming_client_server;

/* Information on the imports used:
   - The first three imports from JUnit are used for setting up,executing, and creating the base of unit tests:
       - @AfterEach: defines methods that are executed after each test case
       - @BeforeEach: defines methods that are executed before each test case
       - @Test: annotate methods that should be treated as test cases
   - The next import from JUnit provides assertion methods to validate test outcomes
   - The following import from Swing is used for creating graphical user interfaces (GUIs) and checking that is working
   - The next import allows for manipulating and accessing fields of private objects through reflection
   - The final import establishes client-server communication:
*/

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import javax.swing.*;
import java.lang.reflect.Field;
import java.io.PrintWriter;
// Ensure that only ChatServer is running to make sure the Test Case are functionally working- it will still run without the server

public class ChatClientTest {
    private ChatClient chatClient;

    @BeforeEach
    public void setUp() throws Exception {
        chatClient = new ChatClient("127.0.0.1");
        // sets up the example client

        // Use reflection to set PrintWriter for testing purposes
        Field printWriterField = ChatClient.class.getDeclaredField("out");
        printWriterField.setAccessible(true);
        printWriterField.set(chatClient, new PrintWriter(System.out, true));
    }

    /*   Test: checks if the userName field in ChatClient is not null and not empty, uses reflection to access the private field userName in ChatClient
         Asserts that:
        - userName is not null (assertNotNull(username))
        - userName is not empty (assertFalse(username.isEmpty()))
    */

    @Test
    public void testValidUsername() throws Exception {
        Field userNameField = ChatClient.class.getDeclaredField("userName");
        userNameField.setAccessible(true);
        String username = (String) userNameField.get(chatClient);

        assertNotNull(username);
        assertFalse(username.isEmpty());
    }

    /* Test: checks that the userPort value is within the valid range of 1024 to 65535 and uses reflection to access the private field userPort in ChatClient
       Asserts that:
        - port number is within the valid range for user-assigned ports: 1024 ≤ port ≤ 65535 (assertTrue(port >= 1024 && port <= 65535))
    */
    @Test
    public void testValidPort() throws Exception {
        Field userPortField = ChatClient.class.getDeclaredField("userPort");
        userPortField.setAccessible(true);
        int port = userPortField.getInt(chatClient);

        assertTrue(port >= 1024 && port <= 65535);
    }

    /* Test: checks that the message area (JTextArea messageArea) exists in the ChatClient and uses reflection to access private messages in ChatClient
    Asserts that:
    - messageArea is not null (assertNotNull(messageArea)
    */
    @Test
    public void testMessageAreaExists() throws Exception {
        Field messageAreaField = ChatClient.class.getDeclaredField("messageArea");
        messageAreaField.setAccessible(true);
        JTextArea messageArea = (JTextArea) messageAreaField.get(chatClient);

        assertNotNull(messageArea);
    }


    @AfterEach
    public void tearDown() {
    }
}

