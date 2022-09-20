package server;

import server.contract.ReadMethods;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocketThread extends Server implements Runnable, ReadMethods {

    private final String DELIMITER = "\r\n\r\n";
    private final Client client;
    private final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

    public SocketThread(Client client) {
        super();
        this.client = client;
    }

    @Override
    public void run() {
        try {
            doHandshake();
            OutputStream out = client.getOut();
            while (true) {
                byte[] message = readMessage(client);
                if (message != null && message.length > 0) {
                    System.out.printf("(%d)[%s]-> %s%n", client.getId(), LocalDateTime.now().format(dateTimeFormat), new String(message, StandardCharsets.UTF_8));
                    sendMessage(out, message);
                }
                out.flush();
            }
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    @Override
    public void sendMessage(OutputStream out, byte[] message) throws IOException {
        int length = message.length;
        if (checkEmptyArray(message)) return;
        int offset = 2;
        byte[] frame = new byte[length + offset];
        frame[0] = (byte) 129;
        frame[1] = (byte) length;
        for (int i = 0; i < frame.length - offset; i++) {
            frame[offset + i] = message[i];
        }

        out.write(frame, 0, frame.length);
        out.flush();
    }

    private boolean checkEmptyArray(byte[] message) {
        byte[] empty = new byte[message.length];
        return Arrays.equals(message, empty);
    }

//    @Override
//    public void sendMessageToOtherClients(Long senderId, String message) {
//        System.out.printf("Client with id: %d sent %s", senderId, message);
//        clients.forEach((id, client) -> {
//            if (!senderId.equals(id)) {
//                OutputStream outStream = client.getOut();
//                PrintWriter writer = new PrintWriter(outStream, true);
//                String clearMessage = message.substring(0, message.indexOf("}") + 1);
//                writer.println(clearMessage);
//                writer.flush();
//            }
//        });
//    }

    @Override
    public byte[] readMessage(Client client) throws IOException {
        DataInputStream input = new DataInputStream(new BufferedInputStream(client.getIn()));
        int packetLength = input.available();

        //Check packet length
        if (packetLength <= 0) return null;

        byte[] data = new byte[packetLength];
        input.read(data);

        int len = data[1] - 128;

        byte[] decoded = null;
        if (len <= 125) {
            len = packetLength;
//            System.out.printf("%d, %d%n", len, packetLength);
            int offset = 6;
            decoded = decodeMessage(data, len, offset);
        } else {
            int offset = 2;
            decoded = decodeMessage(data, len, offset);
        }


        return decoded;
    }

    private void doHandshake() throws IOException, NoSuchAlgorithmException {
        Scanner scanner = new Scanner(client.getIn(), StandardCharsets.UTF_8);

        String data = scanner.useDelimiter(DELIMITER).next();
        Matcher get = Pattern.compile("^GET").matcher(data);

        if (get.find()) {
            Matcher handshakeMatcher = Pattern.compile("Sec-WebSocket-Key: (.*)").matcher(data);
            handshakeMatcher.find();

            String handshakeKey = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-1").digest((handshakeMatcher.group(1) + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11").getBytes()));

            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                    + "Connection: Upgrade\r\n"
                    + "Upgrade: websocket\r\n"
                    + "Sec-WebSocket-Accept: "
                    + handshakeKey
                    + "\r\n\r\n";

            OutputStream output = client.getOut();

            output.write(response.getBytes(), 0, response.length());
        }
    }

    private byte[] decodeMessage(byte[] data, int length, int offset) {
        byte[] key = {data[2], data[3], data[4], data[5]};
        byte[] decoded = new byte[length];
        for (int i = 0; i < decoded.length - offset; i++) {
            decoded[i] = (byte) (data[i + offset] ^ key[i % 4]);
        }

        return decoded;
    }
}
