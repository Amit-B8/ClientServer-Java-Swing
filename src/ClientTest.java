/**
 * The ClientTest class is a driver that will launch the FileClient application
 * It creates a new FileClient window and immediately
 * attempts to connect it to a server running on localhost within the valid
 * port range. This class will start the client
 */

public class ClientTest {
    /**
     * Starts the client program by creating a FileClient instance and calling
     * its method that attempts to connect to the server.
     *
     * @param args not used
     */
    public static void main(String[] args) {
        // Create the client window
        FileClient client = new FileClient();
        // Connect to the server
        client.runClient();
    }
}
