package client;

import java.io.*;
import java.net.Socket;

public class Main {

  private static OutputStream out;
  private static InputStream in;

  public static void main(String[] args) throws IOException {
    /*
    InputStreamReader input = new InputStreamReader(System.in);
    BufferedReader scanner = new BufferedReader(input);

    Socket client = new Socket("localhost", 8085);

    out = client.getOutputStream();
    in = client.getInputStream();

    PrintWriter writer = new PrintWriter(out, true);

    writer.write("Hello world!");
    writer.flush();

    while (true) {
      System.out.print("Message: ");
      String message = scanner.readLine();
      writer.write(message);
      writer.flush();
      System.out.println("Message sent...");
      String receivedMessage = readMessage(client.getInputStream());
      if(!receivedMessage.isEmpty()) System.out.printf("%s%n", receivedMessage);
    }
  }

  protected static String readMessage(InputStream clientInputStream) throws IOException {
    DataInputStream input = new DataInputStream(new BufferedInputStream(clientInputStream));
    StringBuilder sb = new StringBuilder();
    byte[] content = new byte[input.available()];
    input.read(content);
    for (byte c : content) {
      sb.append((char) c);
    }

    return sb.toString();
     */
  }
}
