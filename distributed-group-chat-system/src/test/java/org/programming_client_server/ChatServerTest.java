package org.programming_client_server;
/* Information on the imports used:
   - The first import from JUnit is used to define and execute test cases
   - The next import is used for accessing and manipulating the fields of a class using reflection
   - The final import from JUnit provides assertion methods to validate the expected outcomes of test cases
*/
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class ChatServerTest {
    /*    Verifies that the coordinator field in ChatServer correctly stores and retrieves the assigned value
        -completed (via reflection)
        - sets the coordinator field to "user1" using the setCoordinator("user1") helper method
        - checks the coordinator field using the getCoordinator() helper method
        Asserts that:
        - the value matches the expected value (assertEquals("user1", coordinator))
    */

    @Test
    void testGetCoordinator() throws Exception {
        // set the coordinator via reflection
        setCoordinator("user1");

        // check if the getter retrieves the coordinator correctly
        String coordinator = getCoordinator();
        assertEquals("user1", coordinator);
    }

    // helper method to set the private static 'coordinator' field using reflection
    private void setCoordinator(String coordinatorValue) throws Exception {
        Field coordinatorField = ChatServer.class.getDeclaredField("coordinator");
        coordinatorField.setAccessible(true);  // Make the field accessible
        coordinatorField.set(null, coordinatorValue);  // Set the static field value
    }

    // helper method to get the private static 'coordinator' field using reflection
    private String getCoordinator() throws Exception {
        Field coordinatorField = ChatServer.class.getDeclaredField("coordinator");
        coordinatorField.setAccessible(true);  // Make the field accessible
        return (String) coordinatorField.get(null);  // Get the value of the static field
    }

}
