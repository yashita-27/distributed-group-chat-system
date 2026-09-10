package org.programming_client_server;
/* Information on the imports used:
    - The first two imports are used to implement the GUI features,
    the next three imports are what establishes the network as it
    establishes connections to allow client-server communication,
    -the rest of libraries above are used to help keep track of users,get inputs,
    timestamp messages and creating unique ID for the user
*/

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;
import java.text.SimpleDateFormat;
import java.util.Date;

/*
    The ChatClient class represents the client-side of the chat system which is a key role in how users interact with the system and communicate
    - allows users to connect to the server by providing their username, IP address, and port
    - establishing a GUI to enable users to send and receive messages
    - the user can have both private messaging including a broadcast message to every user in the main chat
    - maintains real-time communication with the server using a separate thread
    - allows user to have actions such as leaving the chat and interacting with the coordinator
*/
public class ChatClient {
    private String serverAddress; // this is the IP address of the server that each user will connect to
    private String userName; // the user
    private int userPort; // this is the port the user will be using to connect to the server
    private String coordinatorName; // this the current coordinator's name
    private Scanner in; // this is the input stream
    private PrintWriter out; // this is te output stream
    private JFrame frame = new JFrame("Group Chat"); // this is titled Group chat for all users as it is the main source of communication
    private JTextField textField = new JTextField();
    private JTextArea messageArea = new JTextArea();
    private JButton sendButton = new JButton("Send"); // the main way a message can be sent
    private JButton leaveButton = new JButton("Leave"); // allow any user to leave including that they can click x on the window
    private JButton contactCoordinatorButton = new JButton("Contact Coordinator"); // allow a user to directly message the coordinator
    private JLabel coordinatorLabel = new JLabel("Coordinator: "); // this will show who is the current coordinator
    private DefaultListModel<String> activeUserListModel = new DefaultListModel<>();
    private JList<String> activeUserList = new JList<>(activeUserListModel); // list all the currently active users
    private JButton privateMessageButton = new JButton("Message Privately"); // allow users to communicate with other users
    private Map<String, JTextArea> privateChats = new HashMap<>(); // this will keep track of the private chats
    private JButton infoButton = new JButton("Information"); // this button will output the username,port and ip address of all active users
    private JButton removeUserButton = new JButton("Remove User"); // this will be used for the coordinator

    /*
        This constructor initialises the ChatClient
        - first it will retrieve the user input (IP address, port number, and username)
        - and after it will then initialise the GUI to allow for user interaction
        - finally it starts a new thread to handle real-time communication with the server
         and the separate thread created means that the GUI remains responsive and the user can see incoming messages from other users
    */
    public ChatClient(String serverAddress) {
        this.serverAddress = serverAddress;
        requestUserInfo();
        chatGUI();
        new Thread(this::run).start();
    }

    /*
    This is how the information is retrieved
    - checks if the userPort is within the allowed bounds of port numbers, it will keep asking until it gets a valid port
    - it asks the user for a username and to make it more unique we decided to add a number between 1000 and 9999 to the name input
    */
    private void requestUserInfo() {
        while (userPort < 1024 || userPort > 65535) {
            String portInput = JOptionPane.showInputDialog(frame, "Enter a valid Port", "Port Address Input", JOptionPane.PLAIN_MESSAGE);
            try {
                userPort = Integer.parseInt(portInput);
                if (userPort < 1024 || userPort > 65535) {
                    throw new NumberFormatException();
                }
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(frame, "Invalid port! Enter a number between 1024 and 65535.");
                userPort = 0;
                // if it was entered incorrectly it will ask the user again
            }
        }
        // the next step is to ask for the name of the user
        do {
            userName = JOptionPane.showInputDialog("Enter your username:");
        } while (userName == null || userName.trim().isEmpty());
        userName = userName.trim() + ThreadLocalRandom.current().nextInt(1000, 9999);
        // this adds the random number
    }

    /*
    this is the method that sets up the Graphical User Interface (GUI) which each client will see and use
    - creates the main chat framework
    - establishes how the active users will be seen in the chat
    - has an input field and has a button to send messages or hitting enter is also a valid way
    - buttons created for more user functionality
    - organises components using panels and layouts to ensure a structured user experience
    */
    private void chatGUI() {
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout());
        Color pinkMainChat = new Color(255, 175, 175);
        frame.getContentPane().setBackground(pinkMainChat);

        // this is where the active users can be seen in the main chat
        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setPreferredSize(new Dimension(150, 0));
        leftPanel.setBorder(BorderFactory.createTitledBorder("Active Users"));
        leftPanel.setBackground(pinkMainChat);
        activeUserList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leftPanel.add(new JScrollPane(activeUserList), BorderLayout.CENTER);
        leftPanel.add(privateMessageButton, BorderLayout.SOUTH);

        // this allows the users to send messages in the main message area
        JPanel centerPanel = new JPanel(new BorderLayout());
        messageArea.setEditable(true);
        centerPanel.setBackground(pinkMainChat);
        centerPanel.add(new JScrollPane(messageArea), BorderLayout.CENTER);
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setBackground(pinkMainChat);
        inputPanel.add(textField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        centerPanel.add(inputPanel, BorderLayout.SOUTH);

        // this is where it will show the current coordinator
        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setPreferredSize(new Dimension(200, 0));
        rightPanel.setBackground(pinkMainChat);
        rightPanel.setBorder(BorderFactory.createTitledBorder("Coordinator"));
        rightPanel.add(coordinatorLabel, BorderLayout.NORTH);
        // within this section is also where the main button components are established
        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(4, 1, 5, 5)); //  easier to group all the buttons together so that they are the same size
        buttonPanel.setBackground(pinkMainChat);
        buttonPanel.add(contactCoordinatorButton); // the main way how they can contact the coordinator
        buttonPanel.add(leaveButton); // allow users to leave the chat
        buttonPanel.add(infoButton); // allow users to see the info
        buttonPanel.add(removeUserButton); // to remove users button
        rightPanel.add(buttonPanel, BorderLayout.SOUTH);

        // the overall frame layout
        frame.add(leftPanel, BorderLayout.WEST);
        frame.add(centerPanel, BorderLayout.CENTER);
        frame.add(rightPanel, BorderLayout.EAST);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // the button functionality is established
        sendButton.addActionListener(e -> sendMessage());
        textField.addActionListener(e -> sendMessage());
        // calling the method send message to allow messages to be sent and received by the users
        leaveButton.addActionListener(e -> leaveChat()); // calls the method to allow a user to exit the whole chat
        contactCoordinatorButton.addActionListener(e -> openContactCoordinator()); // calls the method to establish a private message with the coordinator
        infoButton.addActionListener(e -> displayActiveUsersInformation()) ; // calls the method for users to see all the active user information
        removeUserButton.addActionListener(e -> removeUser());  // calls the method to remove users from the chat (coordinator only)
        privateMessageButton.setEnabled(false);

        // based on the active users you can click on the user that you want to establish a private message
        activeUserList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                privateMessageButton.setEnabled(activeUserList.getSelectedValue() != null);
            }
        });
        // this establishes the private chat after clicking the button by calling the method
        privateMessageButton.addActionListener(e -> {
            String selectedUser = activeUserList.getSelectedValue();
            if (selectedUser != null) {
                openPrivateChatWith(selectedUser);
            }
        });
    }

    /*
    this method is the framework for how private messaging takes place
    - checks if a chat is open
    - establishes the GUI for private messaging to ensure a structured user experience
    */
    private void openPrivateChatWith(String user) {
        if (privateChats.containsKey(user)) {
            return; // if a chat is already open, it does nothing which removes duplicate chats being created
        }
        // the GUI that will be seen and interacted with by the users
        JFrame privateChatFrame = new JFrame("Private Chat with " + user);
        JTextArea chatArea = new JTextArea();
        chatArea.setEditable(false);
        JTextField inputField = new JTextField();
        Color greenPrivateChat = new Color(217, 239, 217);
        chatArea.setBackground(greenPrivateChat);
        // creates the main frame of how it will look
        privateChatFrame.setSize(500, 400);
        privateChatFrame.setLayout(new BorderLayout());
        privateChatFrame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        privateChatFrame.add(inputPanel, BorderLayout.SOUTH);
        privateChatFrame.setVisible(true);
        privateChats.put(user, chatArea);
        JButton sendButton = new JButton("Send");
        // the functionality to allow a user to send a private message as it makes a call to the server to broadcast to a specific user
        sendButton.addActionListener(e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                String timestamp = getCurrentTimestamp();
                out.println("PRIVATE " + user + " " + message);
                chatArea.append(timestamp + " Me: " + message + "\n");
                inputField.setText("");
            }
        });
        // how a user will be able to send messages through inputs and a call to the server so it knows it should only be broadcast to the specific user
        inputField.addActionListener(e -> {
            String message = inputField.getText().trim();
            if (!message.isEmpty()) {
                String timestamp = getCurrentTimestamp();
                out.println("PRIVATE " + user + " " + message);
                chatArea.append(timestamp + " Me: " + message + "\n");
                inputField.setText("");
            }
        });
        // this makes sure to listen for a users actions so if a user leaves it will be in just the private chat and not the main chat
        privateChatFrame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                out.println("PRIVATE_LEAVE " + user);
                privateChats.remove(user);
            }
        });
    }

    /*
    this method handles a private chat with the coordinator
    - checks if a chat is already open to ensure no duplicate chats exist
    - establishes the GUI for private coordinator messaging to ensure a structured user experience
    */
    private void openContactCoordinator() {
        if ((coordinatorName != null) && !coordinatorName.isEmpty()) {
            if (privateChats.containsKey(coordinatorName)) {
                return; // if a chat is already open, it does nothing which removes duplicate chats being created
            }
            // creates the GUI to allow communication
            JFrame privateChatFrame = new JFrame("Private Chat with Coordinator: " + coordinatorName);
            JTextArea chatArea = new JTextArea();
            chatArea.setEditable(false);
            JTextField inputField = new JTextField();
            Color blueCoordChat = new Color(179, 239, 236);
            chatArea.setBackground(blueCoordChat);
            JButton sendButton = new JButton("Send");
            // creates the main look of this specific chat
            privateChatFrame.setSize(500, 400);
            privateChatFrame.setLayout(new BorderLayout());
            privateChatFrame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
            JPanel inputPanel = new JPanel(new BorderLayout());
            inputPanel.add(inputField, BorderLayout.CENTER);
            inputPanel.add(sendButton, BorderLayout.EAST);
            privateChatFrame.add(inputPanel, BorderLayout.SOUTH);
            privateChatFrame.setVisible(true);
            privateChats.put(coordinatorName, chatArea);
            // the functionality to allow a user to send a private message to the coordinator, the same principle as a normal private message
            sendButton.addActionListener(e -> {
                String message = inputField.getText().trim();
                if (!message.isEmpty()) {
                    String timestamp = getCurrentTimestamp();
                    out.println("PRIVATE " + coordinatorName + " " + message);
                    chatArea.append(timestamp + " Me: " + message + "\n");
                    inputField.setText("");
                }
            });
            // the functionality to allow a user to send a private message to the coordinator
            inputField.addActionListener(e -> {
                String message = inputField.getText().trim();
                if (!message.isEmpty()) {
                    String timestamp = getCurrentTimestamp();
                    out.println("PRIVATE " + coordinatorName + " " + message);
                    chatArea.append(timestamp + " Me: " + message + "\n");
                    inputField.setText("");
                }
            });
            // this makes sure to listen for a users actions so if a user leaves it will be in just the private coordinator chat and not the main chat
            privateChatFrame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    out.println("PRIVATE_LEAVE " + coordinatorName);
                    privateChats.remove(coordinatorName);
                }
            });
        } else { // if no coordinator exists then it will create an error
            JOptionPane.showMessageDialog(frame, "No coordinator assigned yet.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /*
       this method handles when a user leaves the chat
        - asks the user if they want to exit the chat
        - notifies the server the user that has left
        - closes the input and output streams for that user
        - it then removes the user
    */
    private void leaveChat() {
        // asks if the user wants to leave the chat
        int confirm = JOptionPane.showConfirmDialog(frame,
                "Are you sure you want to leave the chat?", "Confirm Exit",
                JOptionPane.YES_NO_OPTION);
        // this notifies the server of the user leaving
        if (confirm == JOptionPane.YES_OPTION) {
            if (out != null) {
                out.println(userName + " has left the chat");
            }
            // closes the users stream
            try {
                if (in != null) in.close();
                if (out != null) out.close();
            } catch (Exception ignored) {}
            // this will remove the user from the interface
            activeUserListModel.removeElement(userName);
            frame.dispose();
            System.exit(0);
        }
    }
    /*
       this method is how the user messages are sent
        - retrieves the input the user had
        - displays the message
        - clears the text field so a user can message again
    */
    private void sendMessage() {
        String message = textField.getText().trim();
        if (!message.isEmpty() && out != null) {
            out.println(message);
            textField.setText("");
        }
    }

    // this method is used to show timestamps for all messages
    private String getCurrentTimestamp() {
        SimpleDateFormat timeStamps = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return timeStamps.format(new Date());
    }

    /*
       this method is how the user interaction with the server is handled
        - establishes a connection using the socket
        - sends the user details to the server to store
        - listens for incoming messages from the server and processes different message types:
            - "PRIVATE" → this handles private messages
            - "MESSAGE" → this displays all the general chat messages
            - "PRIVATE_LEAVE" → this notifies when a user leaves a private chat
            - "USERLIST" → this updates the active users list
            - "COORDINATOR_INFO" → this shows the current coordinator
            - "MEMBER_INFO" → this gets and displays details of all active members
        - makes sure that the information is displayed in the GUI correctly and uses SwingUtilities to update
    */
    private void run() {
        try (Socket socket = new Socket(serverAddress, userPort)) { // this is the servers ip address and the port number of the server
            in = new Scanner(socket.getInputStream());
            out = new PrintWriter(socket.getOutputStream(), true); // this is what verifies the connection
            out.println(userName + ":" + serverAddress + ":" + userPort); // sends to the server the details
            while (in.hasNextLine()) {
                String line = in.nextLine();
                // this will allow for private messaging if the user has selected it
                if (line.startsWith("PRIVATE ")) {
                    String[] parts = line.split(" ", 3);
                    String sender = parts[1];
                    String message = parts[2];
                    openPrivateChatWith(sender);
                    privateChats.get(sender).append(sender + ": " + message + "\n");
                } // this will broadcast the message that was sent by the user
                else if (line.startsWith("MESSAGE ")) {
                    messageArea.append(line.substring(8) + "\n");
                } // if a user decides to leave the private chat
                else if (line.startsWith("PRIVATE_LEAVE ")) {
                    String leaver = line.substring(14);
                    if (privateChats.containsKey(leaver)) {
                        privateChats.get(leaver).append(leaver + " has left this chat.\n");
                        privateChats.remove(leaver);
                    }
                } // this will retrieve the currently active users and display them
                else if (line.startsWith("USERLIST")) {
                    updateUserList(line.substring(9));
                } // this is how the coordinator information is retrieved
                else if (line.startsWith("COORDINATOR_INFO")) {
                    coordinatorName = line.substring(16).trim(); // makes sure to store the coordinator
                    SwingUtilities.invokeLater(() -> coordinatorLabel.setText("Coordinator: " + coordinatorName));
                } // this is how the member information is retrieved once a user clicks information
                else if (line.startsWith("MEMBER_INFO ")) {
                    String members = line.substring(12).trim();
                    members = members.replace("\\n", "\n");
                    String[] users = members.split("\\R");
                    StringBuilder formattedUsers = new StringBuilder("Active Users:\n");
                    for (String user : users) {
                        formattedUsers.append(user.trim()).append("\n");
                    }
                    JOptionPane.showMessageDialog(frame, formattedUsers.toString(), "Active Members", JOptionPane.INFORMATION_MESSAGE);
                }

            }
            // this is how it will catch any exceptions and error messages
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
       this method updates the GUI based on the active users
        - retrieves the user list from the server
        - updates the GUI to show any changes
    */
    private void updateUserList(String users) {
        SwingUtilities.invokeLater(() -> { // this is what updates the GUI
            activeUserListModel.clear();
            for (String user : users.split(",")) {
                if (!user.isEmpty()) {
                    activeUserListModel.addElement(user); // adds all the users
                }
            }
        });
    }

    // this method sends this request to the server so that it knows that a user has requested the details which it will send back in the MEMBER_INFO
    private void displayActiveUsersInformation() {
        out.println("REQUEST_MEMBERS");
    }

    // this method retrieves the name of the coordinator
    private boolean isCoordinator() {
        String coordinatorName = coordinatorLabel.getText().replace("Coordinator: ", "").trim();
        return userName.equals(coordinatorName);
    }

    /*
   this method forces a user to be removed (coordinator only)
    - allows the coordinator to enter the username
    - checks if they are sure of this removal
    - if the member is found then REMOVE_MEMBER is called
    - makes sure that only the coordinator can do this
    */
    private void removeUser() {
        if (isCoordinator()) {
            String userToRemove = JOptionPane.showInputDialog(frame, "Enter the username to remove:", "Remove User", JOptionPane.PLAIN_MESSAGE); // entering te user
            if (userToRemove != null && !userToRemove.trim().isEmpty()) {
                if (activeUserListModel.contains(userToRemove.trim())) {
                    // confirmation of the removal
                    int confirm = JOptionPane.showConfirmDialog(frame, "Are you sure you want to remove " + userToRemove + "?", "Confirm Remove", JOptionPane.YES_NO_OPTION);
                    if (confirm == JOptionPane.YES_OPTION) {
                        out.println("REMOVE_MEMBER " + userToRemove.trim()); // this is where they are removed
                    }
                } // error message in case if input wrongly
                else {
                    JOptionPane.showMessageDialog(frame, "User not found, try again", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        } // this outputs a message to the other users so they know that they cant access it
        else {
            JOptionPane.showMessageDialog(frame, "Only the coordinator can remove users", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /*
   this method is the main way of a client connecting
    - asks for the server address
    - after that it instantly starts the user experience by asking for the port number, username and initialising the GUI
    */
    public static void main(String[] args) {
        String serverAddress = JOptionPane.showInputDialog("Enter Server IP Address:");
        new ChatClient(serverAddress);
    }
}