package org.programming_client_server;
/* Information on the imports used:
   - The first import from JUnit is used to specify setup methods that run before each test
   - The second import from JUnit marks the methods as test cases
   - The next import handles output to a stream which is typically used for sending data to clients
   - The following imports is from the Java Collections Framework which stores key-value pairs, and we have used Mockito which is the mocking framework for our unit tests
   - The final import from JUnit provides assertion methods to validate the expected outcomes of test cases
*/

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.PrintWriter;
import java.util.Map;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

// this is the main group class
public class GroupStateTest {
    private GroupState groupState;
    private PrintWriter mockWriter1;
    private PrintWriter mockWriter2;

    @BeforeEach
    public void setUp() {
        // initalise the GroupState and mock PrintWriter objects
        groupState = new GroupState();
        mockWriter1 = mock(PrintWriter.class);
        mockWriter2 = mock(PrintWriter.class);
    }

    @Test
    public void testAddMember_MakesFirstMemberCoordinator() {
        // add the first member and assert that they are the coordinator
        groupState.addMember("user1", "127.0.0.1:5000", mockWriter1);
        // assert that user1 is the coordinator
        assertEquals("user1", groupState.getCoordinator());
    }

    @Test
    public void testAddMember_AdditionalMembers() {
        // add the first member (who becomes the coordinator)
        groupState.addMember("user1", "127.0.0.1:5000", mockWriter1);
        // add another member and check that they are not the coordinator
        groupState.addMember("user2", "127.0.0.1:5001", mockWriter2);
        // assert that the coordinator is still user1
        assertEquals("user1", groupState.getCoordinator());
        // assert that user2 is added to the members map
        assertTrue(groupState.getMembers().containsKey("user2"));
    }


    @Test
    public void testIsMemberActive() {
        // add members to the group
        groupState.addMember("user1", "127.0.0.1:5000", mockWriter1);
        groupState.addMember("user2", "127.0.0.1:5001", mockWriter2);
        // verify that the members are active
        assertTrue(groupState.isMemberActive("user1"));
        assertTrue(groupState.isMemberActive("user2"));
        // removes a member and check again
        groupState.removeMember("user1");
        // verify that user1 is no longer active
        assertFalse(groupState.isMemberActive("user1"));
    }

    @Test
    public void testGetMemberAddress() {
        // add a member to the group
        groupState.addMember("user1", "127.0.0.1:5000", mockWriter1);
        // verify that we can retrieve the address correctly
        assertEquals("127.0.0.1:5000", groupState.getMemberAddress("user1"));
        // checks address for a non-existent member
        assertNull(groupState.getMemberAddress("nonexistentUser"));
    }

    @Test
    public void testGetMembers() {
        // adding members to the group
        groupState.addMember("user1", "127.0.0.1:5000", mockWriter1);
        groupState.addMember("user2", "127.0.0.1:5001", mockWriter2);
        // getting the list of members and assert the correct size and contents
        Map<String, String> members = groupState.getMembers();
        assertEquals(2, members.size());
        assertTrue(members.containsKey("user1"));
        assertTrue(members.containsKey("user2"));
    }

}