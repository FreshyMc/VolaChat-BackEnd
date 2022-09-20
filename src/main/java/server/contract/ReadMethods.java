package server.contract;

import java.io.*;

import server.Client;

public interface ReadMethods {
  void sendMessage(OutputStream out, byte[] message) throws IOException;

//  void sendMessageToOtherClients(Long senderId, String message);

  byte[] readMessage(Client client) throws IOException;
}
