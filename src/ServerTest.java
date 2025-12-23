/**
 * This will run the FileServer program by creating the server window and starting the
 * server so it can accept client connections and handle file upload and retrieval requests
 */

public class ServerTest {
    /**
     * Starts the server program by creating a FileServer instance and calling
     * its method that attempts to start the server
     *
     * @param args not used
     */
    public static void main(String[] args) {
        // This will create the GUI
        FileServer server = new FileServer();
        server.startServer();
    }
}
