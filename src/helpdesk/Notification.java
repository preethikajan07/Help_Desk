package helpdesk;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private final int userId;
    private final String message;
    private final LocalDateTime createdAt = LocalDateTime.now();
    private boolean read;

    public Notification(int id, int userId, String message) {
        this.id = id;
        this.userId = userId;
        this.message = message;
    }

    public int getId() { return id; }
    public int getUserId() { return userId; }
    public String getMessage() { return message; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public boolean isRead() { return read; }
    public void markRead() { this.read = true; }
}
