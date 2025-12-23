// These are the Swing GUI components going to be used
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;



// There are specific AWT classes for the layout management
import java.awt.BorderLayout;
import java.awt.GridLayout;


// Imports specific for event handling interfaces and classes
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

// Import networking and input and output classes
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

// Manage the background tasks
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * FileClient class will allow a single client to upload
 * and retrieve text files from a server.
 * Inherits from JFrame
 *
 * @author Amit Boodhoo
 * @since 2025-11-15
 */
public class FileClient extends JFrame {

    /**
     * Text field for entering the file name
     */
    private JTextField fileNameField;

    /**
     * Text area for viewing and editing file contents
     */
    private JTextArea fileContentArea;

    /**
     * Text area for displaying the connection status and messages
     */
    private JTextArea statusArea;

    /**
     * Button for uploading files
     */
    private JButton uploadButton;
    /**
     * Button for retrieving files
     */
    private JButton retrieveButton;

    /**
     * Networking components for communication with the server
     */
    private Socket socket;
    /**
     * Output stream used for sending text commands to the server.
     */
    private PrintWriter out;
    /**
     * This is the input stream for receiving messages back from the server
     */
    private BufferedReader in;
    /**
     * Executor service is reserved for optional background operations
     */
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    /**
     * This is the first port number the client will attempt to connect to
     */
    // This defines the port range the client can connect to
    // Static and final since it will not change
    private static final int startPort = 23525;
    /**
     * This is the first port number the client will attempt to connect to
     */
    private static final int endPort = 23529;

    /**
     * Creates a new FileClient window and initializes all the graphical components. It
     * includes the file name field, the file content display area, the status
     * message area, and the Upload and Retrieve buttons. The layout is arranged
     * using BorderLayout and GridLayout so that the text areas appear side by
     * side and the control elements are placed at the top and bottom of the
     * window. Action listeners are added so that clicking the button will allow
     * the right command to be sent to the server.
     */
    public FileClient()
    {
        // Calls the parent JFrame constructor to immediately set the window's title during object creation
        super("S57_RemoteWork_Medium: Client");
        // Creates a text field for the user to type a file name
        fileNameField = new JTextField("hello.txt", 20);
        // Creates a large text area for the file's content which is for read and edit
        fileContentArea = new JTextArea(20, 30);
        // This will create a text area for showing connection messages
        statusArea = new JTextArea(20, 30);
        // Sets editable to false so user can’t type anything here
        statusArea.setEditable(false);
        // Create buttons for actions both upload and retrieve
        uploadButton = new JButton("Upload");
        retrieveButton = new JButton("Retrieve");
        // This is the main layout of the window
        setLayout(new BorderLayout(10, 10));
        // This is the Top section for label and filename field
        JPanel topPanel = new JPanel();
        // This is for the small horizontal box at the top
        topPanel.add(fileNameField);
        add(topPanel, BorderLayout.NORTH);
        // This is for the Center section main text areas side by side
        JPanel centerPanel = new JPanel();
        // 1 row, 2 columns, 10 pixel gap horizontally and vertically
        centerPanel.setLayout(new GridLayout(1, 2, 10, 10));
        // Allows the text area JTextArea to scroll when the text becomes longer than the visible area
        centerPanel.add(new JScrollPane(fileContentArea), BorderLayout.CENTER);
        centerPanel.add(new JScrollPane(statusArea), BorderLayout.EAST);
        add(centerPanel, BorderLayout.CENTER);
        // This is for the Bottom section buttons
        JPanel buttonPanel = new JPanel();
        buttonPanel.add(retrieveButton);
        buttonPanel.add(uploadButton);
        add(buttonPanel, BorderLayout.SOUTH);
        // Frame setup
        setSize(800, 600);
        // This will make sure that the entire program exits when the user closes the window.
        // Since without it, closing the window would only hide it, and the app would still run in the background.
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Makes the window visible
        setVisible(true);
        // Only need the listener once for the buttons so do anonymous inner class
        //This is a one time handler for the upload button click.
        uploadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fileName = fileNameField.getText().trim();
                String content = fileContentArea.getText().trim();

                // Only append content if it's not empty
                if (content.trim().isEmpty()) {
                    sendData("UPLOAD " + fileName);
                }
                else {
                    sendData("UPLOAD " + fileName + " " + content);
                }
            }
        });
        // Anonymous inner class for the retrieve button logic
        retrieveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String fileName = fileNameField.getText().trim();
                // Send retrieve request directly in listener
                sendData("RETRIEVE " + fileName);
            }
        });
    }

    /**
     * Attempts to connect to a server on localhost between ports 23525 to 23529.
     * Also start a background task to listen for messages from the server
     */
    public void runClient()
    {
        statusArea.append("Trying to connect to server...\n");
        // Boolean to check if it connects to proper port
        boolean connected = false;
        // Loops through all of my possible ports
        for (int port = startPort; port <= endPort; port++)
        {
            try
            {
                // Creates a new Socket object to connect to the
                // server at localhost on specific port
                socket = new Socket("localhost", port);
                // Creates a PrintWriter object to send text to server
                // 'true' enables auto flush after each println()
                // Without auto flush output may stay in memory instead of reaching the server
                // but flush() makes sure the data is sent immediately it forces any buffered data to be sent right away
                out = new PrintWriter(socket.getOutputStream(), true);
                // Create a new BufferedReader object that wraps an InputStreamReader which reads
                // bytes from the socket's input stream and converts them into characters.
                // This allows reading lines of text from the server and now
                // in holds this BufferedReader object for reading server messages.
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                // Show which specific port it connects too
                statusArea.append("Connected to server at localhost:" + port + "\n");
                // Set boolean to true to show it connected
                connected = true;
                // This is anonymous inner class that is a background listener
                // It constantly waits for server messages and updates the GUI when they arrive
                // Start a background task to listen for messages from the server
                executor.execute(new Runnable()
                {
                    // Overrides the run() method from the Runnable interface not really needed
                    @Override
                    // This defines what this thread will do when started
                    public void run()
                    {
                        try
                        {
                            // Temporary variable to hold each message received from the server
                            String serverMessage;
                            // Continuously read lines from the server
                            while ((serverMessage = in.readLine()) != null)
                            {
                                // Calls displayServerMessage to update the GUI with each new message
                                // Used here for normal server messages that arrive continuously
                                displayServerMessage(serverMessage);
                            }
                        }
                        // Handles the case when server disconnects or input fails
                        catch (IOException e)
                        {
                            // Calls displayServerMessage to show an error message in the GUI
                            // This is used here to notify the user that the connection was lost
                            displayServerMessage("Lost connection to server.");
                        }
                    }
                });

                // Stop looping through ports once connected
                break;
            }
            // Error message if it cannot connect to port
            catch (IOException e)
            {
                statusArea.append("Could not connect on port " + port + ": " + e.getMessage() + "\n");
            }
        }

        // If boolean is false then return that it's unable to connect to any server
        if (connected == false)
        {
            statusArea.append("Unable to connect to any server between ports " + startPort + " and " + endPort + ".\n");
        }
    }

    /**
     * This is a helper method to send messages to the server. It will send a text
     * message to te server if there is a valid connection. The message is displayed in the
     * status area so that the user can see what was sent. Also if the client has not been able to
     * connect to a server then a warning is shown
     *
     * @param message the string command or data to send to the server
     */
    // Private method since it will never be accessed outside of class
    private void sendData(String message) {
        if (out != null)
        {
            // Auto flush is enabled and flush just forces any buffered data to be sent immediately
            out.println(message);
            statusArea.append("CLIENT>>> " + message + "\n");
        }
        else
        {
            statusArea.append("Not connected to a server.\n");
        }
    }

    /**
     * Calls the GUI to display a server message in a safe way.
     * Takes a message from the server and appends it to the status area,
     * and if it is actual file content it will show it in the file content area.
     * Uses SwingUtilities.invokeLater to make sure GUI updates happen on the event-dispatch thread
     * which is the thread in Swing that handles all GUI updates and events.
     *
     * @param msg the server message to display made final because it is used inside an inner class
     */
    private void displayServerMessage(final String msg)
    {
        // Updates the GUI safely in the Swing event thread. Schedule the Runnable to
        // run on the Event Dispatch Thread and runnable is an anonymous inner class implementing Runnable
        SwingUtilities.invokeLater(new Runnable() {
            // Override interface run method which not really needed to write down
            @Override
            // Run method is executed on the event dispatch thread
            public void run() {
                // Append the server message to the statusArea
                statusArea.append("SERVER>>> " + msg + "\n");
                // Update left only for actual file content
                if (msg.startsWith("FILE_CONTENT "))
                {
                    // Create an empty string called content to store the file's actual text
                    String content = "";
                    // Split the server message into at most 3 pieces using spaces as separators.
                    // parts[0] will be "FILE_CONTENT", parts[1] will be the file name, parts[2] will be the file contents.
                    String[] parts = msg.split(" ", 3);
                    // Checks if the message has all three parts
                    if (parts.length >= 3)
                    {
                        // If so only get the contents of the message
                        content = parts[2];
                        // Remove any literal \n in the string
                        content = content.replace("\\n", "");
                    }
                    // Show the content in the left text area of the GUI and if no content it stays blank.
                    fileContentArea.setText(content);
                }
                // Checks is server says the file does not exist
                else if (msg.startsWith("FILE_NOT_FOUND"))
                {
                    // Clear the text area so nothing is shown.
                    fileContentArea.setText("");
                }
            }
        });
    }
}
