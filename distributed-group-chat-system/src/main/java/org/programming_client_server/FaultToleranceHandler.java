package org.programming_client_server;
/* Information on the imports used:
   - these imports are for client-server communication, concurrent execution and I/O operations
*/
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class FaultToleranceHandler {
    private final GroupState groupState;
    private final Timer timer = new Timer();
    private final MessageHandler messageHandler;

    public FaultToleranceHandler(GroupState groupState, MessageHandler messageHandler) {
        this.groupState = groupState;
        this.messageHandler = messageHandler;
        startMonitoring();
    }

    // this method does periodical checks
    private void startMonitoring() {
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkActiveMembers();
            }
        }, 0, 20000);
    }
    // this is the fault tolerance checker for active members
    private void checkActiveMembers() {
        System.out.println("[FaultTolerance] Checking active members...");
        for (String memberID : groupState.getMembers().keySet()) {
            if (!pingMember(memberID)) {
                System.out.println("[FaultTolerance] " + memberID + " is inactive. Removing...");
                groupState.removeMember(memberID);
                messageHandler.broadcastMessage("[SYSTEM] " + memberID + " has left.");
            }
        }
        if (!groupState.isMemberActive(groupState.getCoordinator())) {
            ChatServer.updateCoordinator();

        }
    }
    // this method will check the member pings
    private boolean pingMember(String memberID) {
        String address = groupState.getMemberAddress(memberID);
        if (address == null) {
            return false;
        }
        // this is the user information
        String[] parts = address.split(":");
        String ip = parts[0];
        int port = Integer.parseInt(parts[1]);
        try (Socket socket = new Socket(ip, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner in = new Scanner(socket.getInputStream())) {
            // the message sent to the user
            out.println("PING");
            socket.setSoTimeout(5000);
            return in.hasNextLine() && in.nextLine().equals("RESPONSE"); // the response expected

        } catch (IOException e) {
            return false; // if any exception is needed to be caught
        }
    }

    public void stopMonitoring() {
        timer.cancel();
    }

    //this method is responsible for sending messages to active members
    public static class MessageHandler {
        private final GroupState groupState;
        public MessageHandler(GroupState groupState) {
            this.groupState = groupState;
        }
        // this method broadcasts to any user
        public void broadcastMessage(String message) {
            for (PrintWriter writer : groupState.getMemberWriters().values()) {
                writer.println("MESSAGE " + message);
            }
        }

    }
}