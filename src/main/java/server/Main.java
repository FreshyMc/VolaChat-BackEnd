package server;

import java.io.IOException;

public class Main extends Thread {

  private static final Integer port = 2550;
  private final Server server = new Server(port);
  private static final Long SERVER_RUNTIME = 60000000L;

  public Main() throws IOException {
  }

  public static void main(String[] args) throws InterruptedException, IOException {
    Main main = new Main();
    new Thread(main).start();

    Thread.sleep(SERVER_RUNTIME);

    main.stopServer();
  }

  private void startServer() throws Exception {
    server.run();
  }

  private void stopServer() {
    this.server.stop();
  }

  @Override
  public void run() {
    System.out.println("Starting the server...");
    try {
      this.startServer();
      while (this.server.running) {
        ;
      }
    } catch (IOException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }
    System.out.println("Server stopped!");
  }
}
