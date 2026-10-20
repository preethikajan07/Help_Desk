package helpdesk;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int userId;
    private final String text;
    private final LocalDateTime createdAt = LocalDateTime.now();

    public Comment(int userId, String text) {
        this.userId = userId;
        this.text = text;
    }

    public int getUserId() { return userId; }
    public String getText() { return text; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
