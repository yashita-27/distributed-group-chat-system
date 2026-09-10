package org.programming_client_server;
/* Information on the imports used:
   - these imports store messages to clients and gets the key_values and allows a thread map for active users
*/
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// this is a  thread-safe map to store group members where the key is the member ID and the value is the address for that member
public class GroupState {
    private final Map<String, String> members = new ConcurrentHashMap<>();
    private final Map<String, PrintWriter> memberWriters = new ConcurrentHashMap<>();

    // this finds all the writers for each member
    public Map<String, PrintWriter> getMemberWriters() {
        return memberWriters;
    }

    // this stores the ID of the current coordinator
    private String coordinatorID;

    // this adds a new member to the group the first added member becomes the coordinator
    public synchronized void addMember(String id, String address, PrintWriter writer) {
        members.put(id, address);  // stores the address for later
        memberWriters.put(id, writer);  // stores the writer for message sending
        if (coordinatorID == null) {  // the first member becomes coordinator
            coordinatorID = id;
        }
    }
    // this performs a member removal
    public synchronized void removeMember(String id) {
        members.remove(id);
        memberWriters.remove(id);  // remove the output stream

        if (id.equals(coordinatorID)) {
            ChatServer.updateCoordinator();
        }
    }

    // this checks if a given member is still active in the group
    public synchronized boolean isMemberActive(String id) {
        return members.containsKey(id);
    }

    // this returns the ID of the current coordinator
    public synchronized String getCoordinator() {
        return coordinatorID;
    }

    // this retrieves the current list of group members
    public synchronized Map<String, String> getMembers() {
        return members;
    }

    // retrieves the address of a specified membember
    public synchronized String getMemberAddress(String id) {
        return members.get(id);
    }
}