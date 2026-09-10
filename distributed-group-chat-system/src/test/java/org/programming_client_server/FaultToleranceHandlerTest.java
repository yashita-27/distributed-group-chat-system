package org.programming_client_server;
/* Information on the imports used:
   - The first import from JUnit is used to define setup methods that run before each test
   - The second import from JUnit identifies methods as test cases
   - The next import is used to handle output to a stream which is typically used for sending formatted text
   - The following import allows access to methods through reflection
   - The next two imports from the Java Collections Framework are used for data storage
   - The final import is from Mockito which is used for creating and manipulating the mock objects
*/
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.Mockito.*;

public class FaultToleranceHandlerTest {
    private FaultToleranceHandler faultToleranceHandler;
    private GroupState mockGroupState;
    private FaultToleranceHandler.MessageHandler mockMessageHandler;
    private PrintWriter mockWriter;
    @BeforeEach
    public void setUp() {
        //sets up the initial state of the group
        mockGroupState = mock(GroupState.class);
        mockMessageHandler = mock(FaultToleranceHandler.MessageHandler.class);
        mockWriter = mock(PrintWriter.class); // Mocking the PrintWriter
        // creates the group of members manually using HashMap
        Map<String, String> mockMembers = new HashMap<>();
        mockMembers.put("user1", "127.0.0.1:5000");
        mockMembers.put("user2", "127.0.0.1:5001");
        // mocking the groupState behavior
        when(mockGroupState.getMembers()).thenReturn(mockMembers);
        when(mockGroupState.getMemberAddress(anyString())).thenReturn("127.0.0.1:5000");
        // mocking the getMemberWriters method to return a map of user -> PrintWriter
        Map<String, PrintWriter> mockWriters = new HashMap<>();
        mockWriters.put("user1", mockWriter);
        when(mockGroupState.getMemberWriters()).thenReturn(mockWriters);
        // mocking the coordinator behavior
        when(mockGroupState.getCoordinator()).thenReturn("coordinatorID");
        when(mockGroupState.isMemberActive("coordinatorID")).thenReturn(false);  // Simulate coordinator being inactive
        // creates the FaultToleranceHandler instance with example objects
        faultToleranceHandler = new FaultToleranceHandler(mockGroupState, mockMessageHandler);
        faultToleranceHandler.stopMonitoring();
    }

    /* Test: verifies that inactive members are removed from the group and a system message is broadcast
       - getMemberAddress("user1") returns null which is simulating their inactivity using reflection to call checkActiveMembers
       - Asserts that:
            - removeMember("user1") is called
            - message "[SYSTEM] user1 has left" has been broadcast
    */
    @Test
    public void testCheckActiveMembers_RemovesInactiveMember() throws Exception {
        // simulates an inactive member (pingMember should return false)
        when(mockGroupState.getMemberAddress("user1")).thenReturn(null);
        // accessing the private method
        Method checkActiveMembersMethod = FaultToleranceHandler.class.getDeclaredMethod("checkActiveMembers");
        checkActiveMembersMethod.setAccessible(true); // Make the method accessible
        // calls the method that would call checkActiveMembers indirectly
        checkActiveMembersMethod.invoke(faultToleranceHandler);
        // verify the inactive user
        verify(mockGroupState, atLeastOnce()).removeMember("user1");
        // verify the message "[SYSTEM] user1 has left" has been broadcast
        verify(mockMessageHandler, atLeastOnce()).broadcastMessage("[SYSTEM] user1 has left.");
        // this stops the timer to avoid multiple executions during tests
        faultToleranceHandler.stopMonitoring();
    }
}
