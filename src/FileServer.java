import java.awt.BorderLayout;
// Import for creating the window frame
import javax.swing.JFrame;
import javax.swing.JScrollPane;
// Import for displaying the text inside the window
import javax.swing.JTextArea;
// Import for adding scroll capability to the text area
import javax.swing.JScrollPane;
// Import specific input and output classes for file and network communication
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
// Importing the networking classes for the socket communication
import java.net.ServerSocket;
import java.net.Socket;
// This is to let the server handle clients or background tasks without freezing the GUI
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Formatter;
import java.util.Scanner;


/**
 * The FileServer class provides a GUI based server that accepts a single client
 * connection and allows the client to upload and retrieve text files stored inside the
 * server_files directory. The server automatically selects an available port from range
 * 23525 to 23529 and will display status messages in a scrollable window. It also handles all network
 * communication on background threads so the GUI remains responsive. After a client
 * connects then the server will process commands like Upload and Retrieve and will create or updates
 * files accordingly. It sends responses back to the client and manages all resources before
 * closing the connection.
 */

public class FileServer extends JFrame
{
    // Text area to show messages or updates in the window
    private JTextArea displayArea;
    // This is a server socket which listen for incoming client requests
    private ServerSocket serverSocket;
    // This will represent the connection to a single client
    private Socket connection;
    // This will be used to receive data from the client
    private Scanner  input;
    // Used to send data to the client
    private Formatter  output;
    // Directory where text files are stored and managed
    // final since folder name stays constant for the whole program and will not change
    private static final String fileDirectory = "server_files";
    // This is the ExecutorService used to run server network logic in background threads
    private ExecutorService runServer;

    /**
     * Builds the FileServer window by creating the text area used for displaying
     * server messages and placing it inside a scroll pane. It will also configure the window
     * size and close behavior. It makes the interface visible and creates the
     * server_files directory if it does not already exist so uploaded files
     * have a place to be stored.
     */
    public FileServer()
    {
        // Set the window title by calling the JFrame parent constructor
        super("S57_RemoteWork_Medium: File Server");
        // This will create a text area where server messages and the logs will appear
        displayArea = new JTextArea();
        // Makes the text area uneditable so the users can’t type in it
        displayArea.setEditable(false);
        // This will put the text area inside a scroll pane so the logs can scroll
        JScrollPane scrollPane = new JScrollPane(displayArea);
        // This will add the scroll pane to the center of the window
        add(scrollPane, BorderLayout.CENTER);
        // Set the size of the window
        setSize(400, 300);
        // This will close the program when they press the X button
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Makes the window visible when the program starts
        setVisible(true);
        // This will check to see if the directory exists
        File directory = new File(fileDirectory);
        if (!directory.exists()) {
            directory.mkdir();
            displayArea.append("Created server_files directory.\n");
        }
    }

    /**
     * Starts the server and waits for a client to connect.
     * This is the method that will run after the FileServer constructor sets up the GUI.
     */
    public void startServer() {
        // This will create a pool for background tasks. without freezing the GUI.
        runServer = Executors.newCachedThreadPool();
        // This line starts a background thread that runs the server code.
        // new Runnable() {...} is an anonymous inner class
        // It defines a small piece of code the 'run' method that runs on another thread.
        runServer.execute(new Runnable() {
            @Override
            public void run() {
                try
                {
                    // This is a flag to keep track if it successfully started the server
                    boolean bound = false;
                    // Loops through each prot number from 23525 to 23529
                    for (int port = 23525; port <= 23529; port++) {
                        try
                        {
                            // Creates a ServerSocket object on this port
                            // the number 1 or backlog means only 1 client can queue at a time
                            serverSocket = new ServerSocket(port, 1);
                            // Show a message in window saying server started
                            displayArea.append("Server started on port " + port + "\n");
                            // Marks the bound as true because prot worked
                            bound = true;
                            // Exit the loop since it found a vali port
                            break;
                        }
                        // Runs if the port is already in use or not available and says that
                        catch (IOException e)
                        {
                            displayArea.append("Port " + port + " is in use. Trying next...\n");
                        }
                    }
                    // If bound is false then no available port all failed
                    if (bound == false)
                    {
                        displayArea.append("No available ports between 23525–23529.\n");
                        // This will stop running the server since no valid port was found
                        return;
                    }

                    // Wait for a client to connect.
                    waitForConnection();

                    // Set up input and output streams (to send/receive data).
                    getStreams();

                    // Handle the client's requests (upload/retrieve).
                    processConnection();
                } catch (IOException e) {
                    // This runs if there’s a problem like the port already being used so client disconnects
                    displayArea.append("Error: " + e.getMessage() + "\n");
                } finally {
                    // finally runs even if an exception occurs
                    // It’s used to close resources cleanly
                    closeConnection();
                }
            }
        });
    }

    /**
     * This will wait for a client to connect to the server by blocking on the
     * ServerSocket accept call and displays a message showing that the client connects
     *
     */
    private void waitForConnection() throws IOException
    {
        // Print message to user that the server is starting and is waiting for client
        displayArea.append("Server started. Waiting for a client...");
        // This will be a Block until a client connects. It allows server to accept connection
        // .accept() is form the ServerSocket class
        connection = serverSocket.accept();
        // getInetAddress() returns an InetAddress object representing an IP address
        displayArea.append("Client connected from " + connection.getInetAddress() + "\n");
        System.out.println();
    }

    /**
     * Sets up the input and output streams used for communicating with the
     * client. It uses a Scanner to read incoming messages and a Formatter to send
     * responses and confirms in the display area that the streams are ready.
     */
    public void getStreams() throws IOException
    {
        // This is for the input stream that will read data sent by client
        input = new Scanner(connection.getInputStream());
        // Output stream sends data to client
        output = new Formatter(connection.getOutputStream());
        // Flush will make sure that any buffered data will be sent right away
        output.flush();
        displayArea.append("I/O streams are ready.\n");
    }

    /**
     * This will continuously read messages from the client and handles commands such as
     * UPLOAD and RETRIEVE by saving uploaded text to files. It also returns file
     * contents when requested, updating the server display with activity
     * messages, and sends responses back to the client until the connection ends.
     */
    // Private method since only used in this class
    private void processConnection() throws IOException {
        // Show in the server window that it is ready to handle client messages
        displayArea.append("Ready to process client requests.\n");
        // Keep reading from client as long as connection is open
        while (input.hasNextLine()) {
            // Read the next line of input from the client
            String clientMessage = input.nextLine();
            // Display the message in the server GUI for monitoring
            displayArea.append("Client says: " + clientMessage + "\n");
            // Check if the client wants to upload a file
            if (clientMessage.startsWith("UPLOAD ")) {
                // Split the message into 3 parts: command, filename, and content
                String[] parts = clientMessage.split(" ", 3);
                // If the message is malformed missing filename or content
                if (parts.length < 3)
                {
                    // This is to ignore the malformed message
                    continue;
                }
                // Extract the filename from the message
                String fileName = parts[1];
                // Extract the file content from the message
                String content = parts[2];
                // Create a File object in the server_files folder with this name
                File file = new File(fileDirectory, fileName);
                // Open a BufferedWriter to write text to the file
                // FileWriter lets it write text to the file on the computer
                // BufferedWriter makes writing faster by storing data in memory first
                // try with resources automatically closes the file when done
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(file)))
                {
                    // Write the content to the file overwrites if it exists
                    writer.write(content);
                }
                // Check if there was error writing to file
                catch (IOException e)
                {
                    // Show error if writing fails
                    displayArea.append("Error writing file: " + e.getMessage() + "\n");
                }
                // Send confirmation back to client that file was uploaded
                output.format("FILE_UPLOADED " + fileName + "\n");
                // Make sure the message is sent immediately
                output.flush();
                // Show in server GUI that the file was uploaded
                displayArea.append("Uploaded: " + fileName + "\n");
            }
            // Check if the client wants to retrieve a file
            else if (clientMessage.startsWith("RETRIEVE "))
            {
                // Split into 2 parts command and filename
                String[] parts = clientMessage.split(" ", 2);
                // Ignore malformed retrieve messages
                if (parts.length < 2)
                {
                    continue;
                }
                // Get the filename to retrieve
                String fileName = parts[1];
                // Create a File object in memory pointing to the requested file
                File file = new File(fileDirectory, fileName);
                // Check if the file actually exists
                if (file.exists())
                {
                    // Use StringBuilder to collect all lines from the file
                    StringBuilder sb = new StringBuilder();
                    // Use StringBuilder to collect all lines from the file
                    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                        String line;
                        // Read the file line by line and append to the StringBuilder
                        while ((line = reader.readLine()) != null)
                        {
                            // Append error message if reading fails
                            sb.append(line).append("\n");
                        }
                    }
                    catch (IOException e)
                    {
                        // Append error message if reading fails
                        sb.append("Error reading file: ").append(e.getMessage());
                    }
                    // Replace newlines with literal \n for sending over network
                    String encoded = sb.toString().replace("\n", "\\n");
                    // Send the file content to the client
                    output.format("FILE_CONTENT " + fileName + " " + encoded + "\n");
                    // Make sure the message is sent immediately
                    output.flush();
                }
                else
                {
                    // Inform client that file does not exist
                    output.format("FILE_NOT_FOUND " + fileName + "\n");
                    // Send immediately
                    output.flush();
                }
            }
        }
    }


    /**
     * Closes the connection and streams. This includes the input stream,
     * output stream, and socket. It will also show if there is an error trying to close
     */
    private void closeConnection()
    {
        // This will display a message in the server window when it’s shutting down the connection
        displayArea.append("\nTerminating connection...\n");
        try
        {
            // If the output stream to client was created, then needs to close it to stop sending data
            if (output != null)
                output.close();
            // If the input stream from the client was created, close it to stop reading data
            if (input != null)
                input.close();
            // If the socket connection itself is still open close it to free the network port
            if (connection != null)
                connection.close();
        }
        catch (IOException e)
        {
            // If something goes wrong while closing it will show error
            displayArea.append("Error closing connection: " + e.getMessage() + "\n");
        }
    }
}
