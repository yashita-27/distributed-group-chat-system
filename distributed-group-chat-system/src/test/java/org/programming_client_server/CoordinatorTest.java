package org.programming_client_server;
/* Information on the imports used:
   - The first import from JUnit is used to define setup methods that run before each test
       - @BeforeEach: defines methods that are executed before each test case
   - The second import from JUnit is used to define and execute individual test methods
   - The next import is used for accessing and manipulating the fields of a class using reflection.
   - The next two imports from the Java Collections Framework are used for data storage
   - The following import from JUnit provides assertion methods to validate the expected outcomes of test cases
   - The final import is from Mockito which is used for creating and manipulating the mock objects
*/
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class CoordinatorTest {
    private Coordinator coordinator;
    private GroupState mockGroupState;
    private Map<String, String> mockMembers;

    @BeforeEach
    public void setUp() {
        // this is a mockup of the group state class
        mockGroupState = mock(GroupState.class);
        // creates an example of an active members list
        mockMembers = new HashMap<>();
        mockMembers.put("user1", "127.0.0.1:5000");
        mockMembers.put("user2", "127.0.0.1:5001");
        mockMembers.put("coordinatorID", "127.0.0.1:5555");
        // makes the GroupState return this list
        when(mockGroupState.getMembers()).thenReturn(mockMembers);
        // initalise the Coordinator with the mock GroupState
        coordinator = new Coordinator(mockGroupState, "coordinatorID", "127.0.0.1", 5555);
    }

    // Test: checks if the memberCheck() removes the inactive users correctly
    @Test
    public void testMemberCheckRemovesInactiveUsers() {
        // simulates user2 being inactive
        mockMembers.remove("user2");
        // running the member check
        coordinator.memberCheck();
        // verifies that user2 is no longer in the active list
        assertFalse(mockGroupState.getMembers().containsKey("user2"));
    }

    // Test: ensure that the coordinator is initialised correctly with all the correct information
    @Test
    public void testCoordinatorInitialisation() {
        try {
            // accessing the private fields
            Field coordinatorIdField = Coordinator.class.getDeclaredField("coordinatorID");
            coordinatorIdField.setAccessible(true);
            String coordinatorId = (String) coordinatorIdField.get(coordinator);
            //checks if coordinator is intialised
            assertEquals("coordinatorID", coordinatorId, "Coordinator ID should be initialised correctly.");
            // checks if the coordinators IP is initalised correctly
            Field coordinatorIpField = Coordinator.class.getDeclaredField("coordinatorIP");
            coordinatorIpField.setAccessible(true);
            String coordinatorIP = (String) coordinatorIpField.get(coordinator);
            assertEquals("127.0.0.1", coordinatorIP, "Coordinator IP should be initialised correctly.");
            //checks if coordinator port exists and is initalised correctly
            Field coordinatorPortField = Coordinator.class.getDeclaredField("coordinatorPort");
            coordinatorPortField.setAccessible(true);
            int coordinatorPort = (int) coordinatorPortField.get(coordinator);
            assertEquals(5555, coordinatorPort, "Coordinator Port should be initialised correctly.");
        } catch (NoSuchFieldException | IllegalAccessException e) {
            fail("Failed to access the private fields using reflection.");
        }
    }
}
