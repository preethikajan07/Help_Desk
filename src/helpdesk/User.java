package helpdesk;

import java.io.Serializable;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int id;
    private String name;
    private final String email;
    private String passwordHash;
    private final Role role;

    public User(int id, String name, String email, String passwordHash, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String h) { this.passwordHash = h; }
    public Role getRole() { return role; }
}
