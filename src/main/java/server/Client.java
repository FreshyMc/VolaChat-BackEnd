package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class Client {
    private final Long id;
    private final Socket client;
    private final OutputStream out;
    private final InputStream in;

    public Client(Socket client, Long id) throws IOException {
        this.id = id;
        this.client = client;
        this.out = client.getOutputStream();
        this.in = client.getInputStream();
    }

    public OutputStream getOut() {
        return out;
    }

    public InputStream getIn() {
        return in;
    }

    public Long getId() {
        return id;
    }

    public Socket getClient() {
        return client;
    }
}
