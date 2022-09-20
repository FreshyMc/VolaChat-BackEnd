package server.response;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Heartbeat implements Serializable {
    private String type;
    private String message;

    public Heartbeat(String message) {
        this.message = message;
        this.type = "heartbeat";
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
