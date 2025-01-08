import java.io.*;
import java.net.*;

public class fileserver {
    private static final int PORT = 8081;
    private static final int PACKET_SIZE = 128; 
    private File directory;

    public fileserver(File directory) {
        this.directory = directory;
    }

    public void start() {
        try (DatagramSocket socket = new DatagramSocket(PORT)) {
            System.out.println("UDP File Server started on port " + PORT);

            byte[] buffer = new byte[PACKET_SIZE];
            DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);

            while (true) {
                socket.receive(requestPacket);
                handleRequest(socket, requestPacket);
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        }
    }

    private void handleRequest(DatagramSocket socket, DatagramPacket packet) throws IOException {
        String command = new String(packet.getData(), 0, packet.getLength()).trim();
        InetAddress clientAddress = packet.getAddress();
        int clientPort = packet.getPort();

        if (command.equals("index")) {
            sendFileList(socket, clientAddress, clientPort);
        } else if (command.startsWith("get ")) {
            String fileName = command.substring(4).trim();
            sendFileContent(socket, clientAddress, clientPort, fileName);
        } else {
            sendError(socket, clientAddress, clientPort, "Invalid command");
        }
    }

    private void sendFileList(DatagramSocket socket, InetAddress clientAddress, int clientPort) throws IOException {
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    byte[] data = file.getName().getBytes();
                    DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientPort);
                    socket.send(packet);
                }
            }
        }
        socket.send(new DatagramPacket("end".getBytes(), "end".length(), clientAddress, clientPort));
    }

    private void sendFileContent(DatagramSocket socket, InetAddress clientAddress, int clientPort, String fileName) throws IOException {
        File file = new File(directory, fileName);
        if (!file.exists() || !file.isFile()) {
            sendError(socket, clientAddress, clientPort, "error");
            return;
        }

        byte[] fileData = new byte[PACKET_SIZE - 4];
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            int bytesRead;
            int sequenceNumber = 0;

            while ((bytesRead = in.read(fileData)) != -1) {
                ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
                byteStream.write(intToBytes(sequenceNumber));
                byteStream.write(fileData, 0, bytesRead);

                byte[] packetData = byteStream.toByteArray();
                DatagramPacket packet = new DatagramPacket(packetData, packetData.length, clientAddress, clientPort);
                socket.send(packet);

                if (!awaitAcknowledgment(socket, sequenceNumber)) {
                    System.out.println("Resending packet #" + sequenceNumber);
                    continue;
                }

                sequenceNumber++;
            }
        }
        socket.send(new DatagramPacket("end".getBytes(), "end".length(), clientAddress, clientPort));
    }

    private boolean awaitAcknowledgment(DatagramSocket socket, int sequenceNumber) throws IOException {
        byte[] ackBuffer = new byte[4];
        DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);

        try {
            socket.setSoTimeout(1000); 
            socket.receive(ackPacket); 
            int receivedAck = bytesToInt(ackPacket.getData());
            return receivedAck == sequenceNumber;
        } catch (SocketTimeoutException e) {
            return false;
        } finally {
            socket.setSoTimeout(0);
        }
    }

    private void sendError(DatagramSocket socket, InetAddress clientAddress, int clientPort, String errorMessage) throws IOException {
        byte[] data = errorMessage.getBytes();
        DatagramPacket packet = new DatagramPacket(data, data.length, clientAddress, clientPort);
        socket.send(packet);
    }

    private byte[] intToBytes(int value) {
        return new byte[] {
            (byte) (value >> 24),
            (byte) (value >> 16),
            (byte) (value >> 8),
            (byte) value
        };
    }

    private int bytesToInt(byte[] bytes) {
        return ((bytes[0] & 0xFF) << 24) |
               ((bytes[1] & 0xFF) << 16) |
               ((bytes[2] & 0xFF) << 8) |
               (bytes[3] & 0xFF);
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java fileserver <directory>");
            return;
        }

        File directory = new File(args[0]);
        if (!directory.isDirectory()) {
            System.out.println("Error: specified path is not a directory.");
            return;
        }

        fileserver server = new fileserver(directory);
        server.start();
    }
}


