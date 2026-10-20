package helpdesk;

public enum Status {
    OPEN("Open"), IN_PROGRESS("In Progress"), RESOLVED("Resolved"), CLOSED("Closed");

    private final String label;
    Status(String label) { this.label = label; }
    public String label() { return label; }
}
