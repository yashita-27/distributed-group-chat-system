package org.programming_client_server;
/* Information on the imports used:
   - the first two imports are used for handling networking and server communication including networking (server and client connections) and utilising swing in order to get user input
   - the next set of imports are used for concurrent execution, thread management and timers
   - the last set of imports handle data storage, I/O operations and date/time operations
*/
import java.io.IOException;
import java.io.PrintWriter;
import javax.swing.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
    The ChatServer class represents the server-side of the chat system which is a key role in how systems interacts with the users
    - manages the client connections
    - it tracks the active users interactions by storing their usernames, output streams, as well storing their full details while keeping track of the main coordinator
    - listens to any clients that join on the port
    - allows for multi thread client handling and creating a new one for each client to make tracking easier.
*/
public class ChatServer {
    private static final Set<String> names = new HashSet<>();
    private static final Set<PrintWriter> writers = new HashSet<>();
    private static final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
    private static String coordinator = null;

    // this method initialises the server based on the properties
    public static void main(String[] args) throws Exception {
        int portNumber = 0; // initialise the port
        // checks the port validity
        while (portNumber < 1024 || portNumber > 65535) {
            String portInput = JOptionPane.showInputDialog(null, "Enter a valid Port", "Server Port Input", JOptionPane.PLAIN_MESSAGE);
            try {
                portNumber = Integer.parseInt(portInput);
                if (portNumber < 1024 || portNumber > 65535) {
                    throw new NumberFormatException();
                } // keep asking the user for a valid input until it is valid
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(null, "Invalid port! Enter a number between 1024 and 65535.");
                portNumber = 0; // restart the port number if not valid
            }
        }
        System.out.println("The chat server is running on port " + portNumber + "..."); // this verifies to the user that the port is valid and the server is open
        ExecutorService pool = Executors.newFixedThreadPool(500); // this is the set thread pool but this number can increased/decreased
        try (ServerSocket listener = new ServerSocket(portNumber)) { // listen out for any user interactions with the server
            while (true) {
                pool.execute(new Handler(listener.accept())); // accept any incoming requests from the user
            }
        }
    }

    // this makes sure to send the correct user list to the client so that it can use it to update the GUI
    private static void broadcastUserList() {
        StringBuilder userListString = new StringBuilder("USERLIST ");
        for (String user : names) {
            userListString.append(user).append(",");
        } // this creates the list
        String userListMessage = userListString.toString();
        for (PrintWriter writer : writers) {
            writer.println(userListMessage);
        }
    }

    // this method is what creates the timestamps to make sure that the messages have the correct values
    private static String getCurrentTimestamp() {
        SimpleDateFormat timeStamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return timeStamp.format(new Date());
    }

    // this method handles an update of the coordinator once they leave
    public static void updateCoordinator() {
        synchronized (names) {
            if (!names.isEmpty()) {
                coordinator = names.iterator().next(); // the next available member is chosen
                for (PrintWriter writer : writers) {
                    writer.println("COORDINATOR_INFO " + coordinator); // this will update the information
                    writer.println("MESSAGE [SYSTEM] " + coordinator + " is now the new coordinator."); // this is a message broadcast to all users so they know of the update
                }
            } // if no coordinator can be found then output this message
            else {
                coordinator = null;
                System.out.println("[Server] No coordinator available.");
            }
        }
    }

    /*
    this class method is used to store the client details on the server side
    - it tracks the users information:
       - username,
       - ip address,
       - port number
       - output streams
    */
    static class ClientInfo {
        String user;
        String userAddress;
        int usersPort;
        PrintWriter out;

        public ClientInfo(String name, String ip, int port, PrintWriter out) {
            this.user = name;
            this.userAddress = ip;
            this.usersPort = port;
            this.out = out;
        }
    }

    /*
    this class is used manage a single clients connection about the things that a user wants to do
    - it uses a separate client handler thread
    - stores the connection in order to maintain the connection between the client
    - the main logic for running the client
    */
    private static class Handler implements Runnable {
        private String clientName; // this is a temporary variable that handles the messages
        private final Socket socket;
        private PrintWriter out; //  this will be used to output messages
        // this is the way each thread is maintained
        public Handler(Socket socket) {
            this.socket = socket;
        }
        /* this method manages the communication between the server and a single client
        - 'SUBMITNAME'-  this firstly authenticates the client and assigns a unique username
        - 'USERLIST' tracks connected users and maintains a list of active members
        - 'PRIVATE' and 'MESSAGE' handle the message exchange
            - Private messages (sent directly to a specific user)
            - Public messages (broadcast to all users)
        - allows clients to request information as well as actions:
            - 'REQUEST_MEMBERS' keeps track of active members
            - 'REMOVE_MEMBER' allows the coordinator to remove users from the chat
        - it handles user disconnection by referencing the method
            */

        public void run() {
            try {
                Scanner in = new Scanner(socket.getInputStream()); // sets up the input streams for the client
                out = new PrintWriter(socket.getOutputStream(), true); //sets up the output streams for the client
                // registers the users details (name)
                while (true) {
                    out.println("SUBMITNAME");
                    String clientInfo = in.nextLine();
                    String[] parts = clientInfo.split(":");
                    clientName = parts[0];
                    String ip = parts[1];
                    int clientPort = Integer.parseInt(parts[2]);
                    // checks if the username is unique and then creates the client
                    synchronized (names) {
                        if (!clientName.isEmpty() && !names.contains(clientName)) {
                            names.add(clientName);
                            clients.put(clientName, new ClientInfo(clientName, ip, clientPort, out));
                            // makes sure that the first user is the coordinator
                            if (coordinator == null) {
                                coordinator = clientName;
                            }
                            out.println("COORDINATOR_INFO " + coordinator);
                            break; // if it is a valid username, and they are registered then the user can interact with the GUI
                        }
                    }
                }
                // this notifies the user, and it will update the user list
                out.println("NAMEACCEPTED " + clientName);
                writers.add(out);
                broadcastUserList();
                out.println("USERLIST " + String.join(",", names));
                if (coordinator != null) {
                    out.println("COORDINATOR_INFO " + coordinator); // this notifies about the coordinator as well
                }
                // this is a message broadcast to everyone once a new user joins
                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + getCurrentTimestamp() + " " + clientName + " has entered the chat.");
                }
                // this listens for any messages from the user
                while (true) {
                    String input = in.nextLine();
                    if (input.toLowerCase().startsWith("/quit")) {
                        return; // if the user wants to quit then they can exit the chat
                    } // this handles the private messages between the users
                    else if (input.startsWith("PRIVATE ")) {
                        String[] parts = input.split(" ", 3);
                        if (parts.length < 3) continue;
                        String recipient = parts[1]; // this is the user who will receive the message
                        String message = parts[2];  // the contents of the message
                        ClientInfo recipientClient = clients.get(recipient);
                        if (recipientClient != null) {
                            recipientClient.out.println("PRIVATE " + clientName + " " + getCurrentTimestamp() + " " + message);
                        }
                    } // this handles getting the user information and building it up
                    else if (input.startsWith("REQUEST_MEMBERS")) {
                        StringBuilder userDetails = new StringBuilder("MEMBER_INFO ");
                        if (clients.isEmpty()) {
                            userDetails.append("No active users available."); // if no users are available
                        } else {
                            for (ClientInfo client : clients.values()) {
                                userDetails.append(client.user) // for the specific user
                                        .append(", IP Address: ") // gets the stored user ip address
                                        .append(client.userAddress)
                                        .append(", Port: ") // gets the stored user port number
                                        .append(client.usersPort)
                                        .append(" \\n ");
                            }
                        }
                        out.println(userDetails.toString().trim()); // this makes sure it is a valid output and gives it to the client
                    } // if the client wants to leave, this is the notification received
                    else if (input.startsWith("PRIVATE_LEAVE ")) {
                        String[] parts = input.split(" ", 2);
                        if (parts.length < 2) continue;
                        String leavingUser = parts[1];
                        ClientInfo recipientClient = clients.get(leavingUser);
                        if (recipientClient != null) {
                            recipientClient.out.println("PRIVATE_LEAVE " + clientName); // based on the client username
                        }
                    } // this will remove a member if the coordinator has made this request
                    else if (input.startsWith("REMOVE_MEMBER ")) {
                        String[] parts = input.split(" ", 2);
                        if (parts.length < 2) continue;
                        String memberToRemove = parts[1].trim();
                        if (clientName.equals(coordinator) && clients.containsKey(memberToRemove)) { // checks if is the coordinator doing this
                            PrintWriter removedClientWriter = clients.get(memberToRemove).out;
                            removedClientWriter.println("MESSAGE [SYSTEM] You have been removed from the chat."); // this is output to the user in main chat
                            removedClientWriter.close();
                            names.remove(memberToRemove);
                            clients.remove(memberToRemove);
                            writers.remove(removedClientWriter);
                            // this removes the user from the user list, tracking, and broadcast list so they can't do anything as they are removed
                            handleClientExit(memberToRemove); // makes sure it is a proper exit
                            if (memberToRemove.equals(coordinator)) {
                                updateCoordinator(); // if the coordinator wants to remove themselves then reassign
                            }
                            broadcastUserList();
                            for (PrintWriter writer : writers) {
                                writer.println("MESSAGE [SYSTEM] " + memberToRemove + " has been removed from the chat.");
                                // this verifies that the user has been removed to the other users
                            }
                        } else { // if any user other than the coordinator tries to remove a member
                            out.println("MESSAGE [SYSTEM] Only the coordinator can remove members.");
                        }
                    } // otherwise treat this as a normal message and allow for communication
                    else {
                        for (PrintWriter writer : writers) {
                            writer.println("MESSAGE " + getCurrentTimestamp() + " " + clientName + ": " + input);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println(e); // this is a print for debugging
            } finally {
                handleClientExit(); // this ensures that a clients exit is handled
            }
        }

        // this method handles cases of what type of user is leaving
        private static void handleClientExit(String exitingClient) {
            if (exitingClient.equals(coordinator)) {
                updateCoordinator(); // this is called when the coordinator has left
            }
            broadcastUserList(); // makes sure to update the user list
        }
        // this is for a regular user who wants to leave
        private void handleClientExit() {
            if (out != null) writers.remove(out);
            if (clientName != null) {
                names.remove(clientName); // removes the stored name
                clients.remove(clientName); // removes the stored information
                broadcastUserList(); // makes sure to update the user list
                // this notifies to all users that a specific user has left
                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + clientName + " has left the chat.");
                } // if the leaving member is the coordinator
                if (clientName.equals(coordinator) && !names.isEmpty()) {
                    updateCoordinator(); // this makes a call to update the coordinator
                }
            }
            try {
                socket.close(); // this closes the connection once a client disconnects or the server has been shut down
            } catch (IOException e) {
                e.printStackTrace();
            } // catching any errors that may be stopping it from closing
        }
    }
}