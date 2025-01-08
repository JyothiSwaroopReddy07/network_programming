import java.io.*;
import java.net.*;
import java.util.Arrays;

public class fileclient {
    private static final int PORT = 8081; // Server port
    private static final int PACKET_SIZE = 128; // Packet size
    private static final int TIMEOUT = 2000; // 2 seconds timeout for receiving data

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java fileclient <server> <command>");
            System.out.println("Commands:");
            System.out.println("  index              - List files on the server");
            System.out.println("  get <filename>     - Download a file from the server");
            return;
        }

        String serverAddress = args[0];
        String command = args[1];

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT); // Set socket timeout
            InetAddress address = InetAddress.getByName(serverAddress);

            sendCommand(socket, address, command);
            if (command.equals("index")) {
                receiveFileList(socket);
            } else if (command.startsWith("get ")) {
                String fileName = command.substring(4).trim();
                receiveFile(socket, fileName);
            } else {
                System.out.println("Invalid command.");
            }
        } catch (IOException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void sendCommand(DatagramSocket socket, InetAddress address, String command) throws IOException {
        byte[] commandData = command.getBytes();
        DatagramPacket commandPacket = new DatagramPacket(commandData, commandData.length, address, PORT);
        socket.send(commandPacket);
    }

    private static void receiveFileList(DatagramSocket socket) throws IOException {
        System.out.println("Files on server:");
        byte[] buffer = new byte[PACKET_SIZE];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

        while (true) {
            try {
                socket.receive(packet);
                String response = new String(packet.getData(), 0, packet.getLength()).trim();
                if (response.equals("end")) { // End of list signal
                    break;
                }
                System.out.println(response);
            } catch (SocketTimeoutException e) {
                System.out.println("Timed out waiting for file list.");
                break;
            }
        }
    }

    private static void receiveFile(DatagramSocket socket, String fileName) {
        try (FileOutputStream fileOut = new FileOutputStream("downloaded_" + fileName)) {
            byte[] buffer = new byte[PACKET_SIZE];
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            int expectedSequence = 0;

            while (true) {
                try {
                    socket.receive(packet);
                    byte[] data = packet.getData();
                    String response = new String(data, 0, packet.getLength()).trim();
                    System.out.println(response);
                    if (response.equals("end")) { // End of file signal
                        System.out.println("File transfer complete.");
                        break;
                    } else if (response.equals("error")) {
                        System.out.println("File not found on server.");
                        break;
                    }

                    int sequenceNumber = bytesToInt(Arrays.copyOfRange(data, 0, 4));
                    byte[] fileData = Arrays.copyOfRange(data, 4, packet.getLength());

                    if (sequenceNumber == expectedSequence) {
                        fileOut.write(fileData); // Write chunk to file
                        sendAcknowledgment(socket, packet.getAddress(), packet.getPort(), sequenceNumber);
                        expectedSequence++; // Move to the next sequence
                    } else {
                        System.out.println("Unexpected sequence number. Resending ACK for sequence #" + (expectedSequence - 1));
                        sendAcknowledgment(socket, packet.getAddress(), packet.getPort(), expectedSequence - 1); // Resend last ACK
                    }

                } catch (SocketTimeoutException e) {
                    System.out.println("Timed out waiting for file chunk. Retrying...");
                }
            }
        } catch (IOException e) {
            System.err.println("Error receiving file: " + e.getMessage());
        }
    }

    private static void sendAcknowledgment(DatagramSocket socket, InetAddress address, int port, int sequenceNumber) throws IOException {
        byte[] ackData = intToBytes(sequenceNumber);
        DatagramPacket ackPacket = new DatagramPacket(ackData, ackData.length, address, port);
        socket.send(ackPacket);
    }

    private static byte[] intToBytes(int value) {
        return new byte[] {
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) value
        };
    }

    private static int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
               ((bytes[1] & 0xFF) << 16) |
               ((bytes[2] & 0xFF) << 8) |
               (bytes[3] & 0xFF);
    }
}
