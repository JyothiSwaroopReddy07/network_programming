import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.stream.Collectors;

public class fileserver {
    private static final int PORT = 8080;
    private final File directory;

    public fileserver(String directoryName) {
        this.directory = new File(directoryName);
        if (!directory.isDirectory()) {
            throw new IllegalArgumentException("Provided path is not a directory.");
        }
    }

    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("FileServer started. Listening on port " + PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Client connected: " + clientSocket.getInetAddress());

                    handleClient(clientSocket);
                } catch (IOException e) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not start the server: " + e.getMessage());
        }
    }

    private void handleClient(Socket clientSocket) throws IOException {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String command = in.readLine();
            if (command == null) return;

            if (command.equals("index")) {
                sendFileList(out);
            } else if (command.startsWith("get ")) {
                String fileName = command.substring(4).trim();
                sendFileContent(out, fileName);
            } else {
                out.println("error");
            }
        }
    }

    private void sendFileList(PrintWriter out) {
        String fileList = listFilesInDirectory();
        out.println(fileList);
    }

    private String listFilesInDirectory() {
        return Arrays.stream(directory.listFiles())
                .filter(File::isFile)
                .map(File::getName)
                .collect(Collectors.joining("\n"));
    }

    private void sendFileContent(PrintWriter out, String fileName) {
        File file = new File(directory, fileName);

        if (file.exists() && file.isFile()) {
            out.println("ok");

            try (BufferedReader fileReader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = fileReader.readLine()) != null) {
                    out.println(line);
                }
            } catch (IOException e) {
                System.err.println("Error reading file " + fileName + ": " + e.getMessage());
                out.println("error");
            }
        } else {
            out.println("error");
        }
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java FileServer <directory>");
            return;
        }

        String directoryName = args[0];
        fileserver server = new fileserver(directoryName);
        server.start();
    }
}
