package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import server.contract.ReadMethods;
import server.response.Heartbeat;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SocketThread extends Server implements Runnable, ReadMethods {

    private final String DELIMITER = "\r\n\r\n";
    private final Client client;
    private final DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
    private final Gson gson = new GsonBuilder().setLenient().create();
    private final Long heartbeatMessagePeriod = 5000L;
    private final Long tolerance = 20000L;
    private static Long millis = System.currentTimeMillis();
    private static Long lastHeartbeatMessageReceived = System.currentTimeMillis();

    public SocketThread(Client client) {
        super();
        this.client = client;
    }

    @Override
    public void run() {
        try {
            doHandshake();
            OutputStream out = client.getOut();
            Socket c = client.getClient();
            while (true) {
                byte[] message = readMessage(client);
                if (message != null && message.length > 0) {
                    System.out.printf("(%d)[%s]-> %s%n", client.getId(), LocalDateTime.now().format(dateTimeFormat), new String(message, StandardCharsets.UTF_8));
//                    sendMessage(out, message);
                    sendMessageToOtherClients(client.getId(), message);
                } else if(System.currentTimeMillis() - millis >= heartbeatMessagePeriod) {
                    sendBeat(out);
                    millis = System.currentTimeMillis();

                    if(System.currentTimeMillis() - lastHeartbeatMessageReceived > tolerance) break;
                }
                out.flush();
            }
            closeConnection();
        } catch (Exception ex) {
            Logger.getAnonymousLogger().log(Level.INFO, ex.getMessage());
        }finally {
            try {
                closeConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
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

        String message = new String(decoded, StandardCharsets.UTF_8).trim();
        Heartbeat heartbeatResponse = gson.fromJson(message, Heartbeat.class);
        if(heartbeatResponse.getType().equals("heartbeat") && heartbeatResponse.getMessage().equals("pong")){
            System.out.println("Client is alive...");
            lastHeartbeatMessageReceived = System.currentTimeMillis();
            return null;
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

    private void sendBeat(OutputStream out) throws IOException {
        Heartbeat ping = new Heartbeat("ping");
        byte[] heartbeatMessage = gson.toJson(ping).getBytes(StandardCharsets.UTF_8);
        sendMessage(out, heartbeatMessage);
    }

    private void closeConnection() throws IOException {
        //Close connection
        client.getIn().close();
        client.getOut().close();
        client.getClient().close();
        for (Map.Entry<Long, Client> clientToRemove : super.clients.entrySet()) {
            if(clientToRemove.getKey().equals(client.getId())) {
                clients.remove(clientToRemove);
                break;
            }
        }
        System.out.println("Client has disconnected...");
    }

    private void sendMessageToOtherClients(Long senderId, byte[] message) throws IOException {
        //System.out.printf("Client with id: %d sent %s", senderId, new String(message, StandardCharsets.UTF_8).trim());
        System.out.println(super.clients.keySet());
        for (Map.Entry<Long, Client> client : super.clients.entrySet()) {
            if(!client.getKey().equals(senderId)) {
                sendMessage(client.getValue().getOut(), message);
                System.out.printf("Sent to client: %d%n", client.getKey());
            }
        }
    }
}
