import java.io.*;
import java.net.*;

public class fileclient {
    private static final int PORT = 8080; 

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java FileClient <server> <command>");
            return;
        }

        String serverAddress = args[0];
        String command = args[1];

        try (Socket socket = new Socket(serverAddress, PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println(command); 

            String response;
            while ((response = in.readLine()) != null) { 
                System.out.println(response);
            }

        } catch (IOException e) {
            System.err.println("Error connecting to server: " + e.getMessage());
        }
    }
}
