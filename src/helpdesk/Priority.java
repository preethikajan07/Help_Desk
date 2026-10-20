package helpdesk;

public enum Priority {
    LOW, MEDIUM, HIGH;

    public String label() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
