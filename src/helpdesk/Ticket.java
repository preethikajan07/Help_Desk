package helpdesk;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Ticket implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private String title;
    private String description;
    private Priority priority;
    private Status status = Status.OPEN;
    private final int createdBy;
    private Integer assignedTo;
    private final LocalDateTime createdAt;
    private final List<Comment> comments = new ArrayList<>();

    public Ticket(int id, String title, String description, Priority priority,
                  int createdBy, LocalDateTime createdAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority p) { this.priority = p; }
    public Status getStatus() { return status; }
    public void setStatus(Status s) { this.status = s; }
    public int getCreatedBy() { return createdBy; }
    public Integer getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Integer staffId) { this.assignedTo = staffId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public List<Comment> getComments() { return comments; }
}
