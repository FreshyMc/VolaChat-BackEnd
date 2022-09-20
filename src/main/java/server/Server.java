package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class Server {

  private ServerSocket socket;
  public boolean running = true;
  protected final Map<Long, Client> clients = new HashMap<Long, Client>();
  private static Long clientCount = 0L;

  public Server() {
  }

  public Server(Integer port) throws IOException {
    this.socket = new ServerSocket(port);
  }

  public void run() throws Exception {
    System.out.println("Server started!");
    while (running) {
      Socket client = socket.accept();

      if (client != null) {
        Client connObject = new Client(client, clientCount++);
        System.out.println("Client has connected!");
        addClient(connObject);
        SocketThread connection = new SocketThread(connObject);
        new Thread(connection).start();
      }
    }
  }

  public void stop() {
    this.running = false;
  }

  private void addClient(Client client) {
    clients.put(client.getId(), client);
  }
}
