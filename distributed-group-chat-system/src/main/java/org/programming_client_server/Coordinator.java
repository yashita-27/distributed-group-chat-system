package org.programming_client_server;
/* Information on the imports used:
   - this is for handling networking and server communication, concurrent execution and I/O operations
*/
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

/*
    This constructor initialises the Coordinator
    - it stores the coordinators ID, IP address, and port number
    - this is linked to the shared GroupState object, which tracks active users
    - starts a  periodic member checks to monitor the active users and removes those that are not active
    - if the coordinator disconnects a new one will be assigned in the system
*/

public class Coordinator {
    private String coordinatorID;
    private String coordinatorIP;
    private int coordinatorPort;
    private final Timer timer = new Timer(); // this will be used to periodically check the times
    private final GroupState groupState; // maintain the state

    public Coordinator(GroupState groupState, String id, String ip, int port) {
        this.groupState = groupState;
        this.coordinatorID = id;
        this.coordinatorIP = ip;
        this.coordinatorPort = port;
        memberCheck();
    }
    // this method will check for the active users and checks if they are sending a response
    public void memberCheck() {
        timer.scheduleAtFixedRate(new TimerTask() { // checks based on the specified time
            @Override
            public void run() {
                System.out.println("[Coordinator] Checking active members..."); // this is when it is checking the members
                Iterator<Map.Entry<String, String>> iterator = groupState.getMembers().entrySet().iterator(); // this make sure to go to the next user in the active users
                while (iterator.hasNext()) {
                    Map.Entry<String, String> entry = iterator.next();
                    String memberID = entry.getKey();
                    String memberAddress = entry.getValue();
                    String[] addressParts = memberAddress.split(":");
                    String ip = addressParts[0];
                    int port = Integer.parseInt(addressParts[1]);
                    // ensures that all the user details are retrieved
                    try (Socket socket = new Socket(ip, port);
                         PrintWriter out = new PrintWriter(socket.getOutputStream(), true); // using the otuput stream
                         Scanner in = new Scanner(socket.getInputStream())) { // checks the input stream
                        out.println("PING"); // this sends a ping to the client
                        socket.setSoTimeout(50000); // this is the max limit (50 seconds) for it to be delivered
                        if (in.hasNextLine() && in.nextLine().equals("RESPONSE")) { // if a response is received
                            System.out.println("[Coordinator] Member " + memberID + " is active."); // the user is active
                        } else {
                            throw new IOException("No RESPONSE received"); // else this will give this exception
                        }
                    } catch (Exception e) { // if this exception is caught then the user is marked to be removed
                        System.out.println("[Coordinator] Member " + memberID + " is inactive. Removing...");
                        iterator.remove(); // it will remove the user
                    }
                }
                if (!groupState.getMembers().containsKey(coordinatorID) && !groupState.getMembers().isEmpty()) {
                    ChatServer.updateCoordinator();
                    sendMessageToMember(coordinatorID,"MESSAGE [SYSTEM] You are now the coordinator.");
                }
            }
        }, 0, 20000); // this is the time specified
    }

    // this is when a message has failed to send to a member
    private void sendMessageToMember(String memberID, String message) {
        String address = groupState.getMembers().get(memberID);
        String[] parts = address.split(":");
        try (Socket socket = new Socket(parts[0], Integer.parseInt(parts[1]));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(message);
        } catch (IOException e) {
            System.out.println("[Coordinator] Failed to send message to " + memberID);
        }
    }

}