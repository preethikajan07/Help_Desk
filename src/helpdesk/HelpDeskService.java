package helpdesk;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** All business rules: users, tickets, comments, assignment, notifications. */
public class HelpDeskService {
    private final Database db;
    private final String path;

    public HelpDeskService(Database db, String path) {
        this.db = db;
        this.path = path;
    }

    private void save() { db.save(path); }

    // ---------- security ----------
    public static String hash(String plain) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(plain.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte x : b) sb.append(String.format("%02x", x));
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- users ----------
    public User register(String name, String email, String password, Role role) {
        if (name.isBlank() || email.isBlank() || password.length() < 4)
            throw new IllegalArgumentException("Name/email required and password must be at least 4 characters.");
        if (findUserByEmail(email).isPresent())
            throw new IllegalArgumentException("That email is already registered.");
        User u = new User(db.nextUserId++, name.trim(), email.trim().toLowerCase(), hash(password), role);
        db.users.add(u);
        save();
        return u;
    }

    public Optional<User> login(String email, String password) {
        return findUserByEmail(email).filter(u -> u.getPasswordHash().equals(hash(password)));
    }

    public Optional<User> findUserByEmail(String email) {
        return db.users.stream().filter(u -> u.getEmail().equalsIgnoreCase(email.trim())).findFirst();
    }

    public Optional<User> findUser(int id) {
        return db.users.stream().filter(u -> u.getId() == id).findFirst();
    }

    public String nameOf(Integer id) {
        if (id == null) return "-";
        return findUser(id).map(User::getName).orElse("Unknown");
    }

    public List<User> allUsers() { return db.users; }

    public List<User> staffMembers() {
        return db.users.stream().filter(u -> u.getRole() == Role.STAFF).collect(Collectors.toList());
    }

    public void deleteUser(User actor, int userId) {
        requireRole(actor, Role.ADMIN);
        if (actor.getId() == userId) throw new IllegalArgumentException("You cannot delete yourself.");
        User target = findUser(userId).orElseThrow(() -> new IllegalArgumentException("User not found."));
        db.users.remove(target);
        for (Ticket t : db.tickets)
            if (t.getAssignedTo() != null && t.getAssignedTo() == userId) t.setAssignedTo(null);
        save();
    }

    // ---------- tickets ----------
    public Ticket createTicket(User user, String title, String description, Priority p) {
        if (title.isBlank() || description.isBlank())
            throw new IllegalArgumentException("Title and description cannot be empty.");
        Ticket t = new Ticket(db.nextTicketId++, title.trim(), description.trim(), p, user.getId(), LocalDateTime.now());
        db.tickets.add(t);
        for (User admin : db.users)
            if (admin.getRole() == Role.ADMIN)
                notify(admin.getId(), "New ticket #" + t.getId() + " \"" + t.getTitle() + "\" was created by " + user.getName() + ".");
        notify(user.getId(), "Your ticket #" + t.getId() + " has been created.");
        save();
        return t;
    }

    /** Customers see only their tickets; staff and admin see all. */
    public List<Ticket> ticketsFor(User user) {
        return db.tickets.stream()
                .filter(t -> user.getRole() != Role.CUSTOMER || t.getCreatedBy() == user.getId())
                .sorted(Comparator.comparing(Ticket::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public List<Ticket> assignedTo(User staff) {
        return db.tickets.stream()
                .filter(t -> t.getAssignedTo() != null && t.getAssignedTo() == staff.getId())
                .sorted(Comparator.comparing(Ticket::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public Ticket getTicket(User viewer, int id) {
        Ticket t = db.tickets.stream().filter(x -> x.getId() == id).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Ticket #" + id + " not found."));
        if (viewer.getRole() == Role.CUSTOMER && t.getCreatedBy() != viewer.getId())
            throw new IllegalArgumentException("Ticket #" + id + " not found.");
        return t;
    }

    public List<Ticket> search(User viewer, String keyword) {
        String k = keyword.toLowerCase();
        return ticketsFor(viewer).stream()
                .filter(t -> t.getTitle().toLowerCase().contains(k) || t.getDescription().toLowerCase().contains(k))
                .collect(Collectors.toList());
    }

    public List<Ticket> filterByStatus(User viewer, Status s) {
        return ticketsFor(viewer).stream().filter(t -> t.getStatus() == s).collect(Collectors.toList());
    }

    public void updateStatus(User actor, int ticketId, Status status) {
        if (actor.getRole() == Role.CUSTOMER) throw new SecurityException("Only staff or admin can change status.");
        Ticket t = getTicket(actor, ticketId);
        t.setStatus(status);
        notify(t.getCreatedBy(), "Your ticket #" + t.getId() + " is now " + status.label() + ".");
        save();
    }

    public void assign(User actor, int ticketId, int staffId) {
        requireRole(actor, Role.ADMIN);
        Ticket t = getTicket(actor, ticketId);
        User staff = findUser(staffId).filter(u -> u.getRole() == Role.STAFF)
                .orElseThrow(() -> new IllegalArgumentException("No staff member with id " + staffId + "."));
        t.setAssignedTo(staff.getId());
        if (t.getStatus() == Status.OPEN) t.setStatus(Status.IN_PROGRESS);
        notify(staff.getId(), "Ticket #" + t.getId() + " has been assigned to you.");
        notify(t.getCreatedBy(), "Your ticket #" + t.getId() + " has been assigned to a staff member.");
        save();
    }

    public void addComment(User actor, int ticketId, String text) {
        if (text.isBlank()) throw new IllegalArgumentException("Comment cannot be empty.");
        Ticket t = getTicket(actor, ticketId);
        t.getComments().add(new Comment(actor.getId(), text.trim()));
        Set<Integer> targets = new HashSet<>();
        targets.add(t.getCreatedBy());
        if (t.getAssignedTo() != null) targets.add(t.getAssignedTo());
        targets.remove(actor.getId());
        for (int id : targets) notify(id, "A new comment was added to ticket #" + t.getId() + ".");
        save();
    }

    public void deleteTicket(User actor, int ticketId) {
        requireRole(actor, Role.ADMIN);
        Ticket t = getTicket(actor, ticketId);
        db.tickets.remove(t);
        save();
    }

    // ---------- dashboard ----------
    public Map<String, Long> stats(List<Ticket> tickets) {
        Map<String, Long> m = new LinkedHashMap<>();
        m.put("Total Tickets", (long) tickets.size());
        m.put("Open", tickets.stream().filter(t -> t.getStatus() == Status.OPEN).count());
        m.put("In Progress", tickets.stream().filter(t -> t.getStatus() == Status.IN_PROGRESS).count());
        m.put("Resolved", tickets.stream().filter(t -> t.getStatus() == Status.RESOLVED).count());
        return m;
    }

    // ---------- notifications ----------
    private void notify(int userId, String message) {
        db.notifications.add(new Notification(db.nextNotificationId++, userId, message));
    }

    public List<Notification> notificationsFor(User u) {
        return db.notifications.stream().filter(n -> n.getUserId() == u.getId())
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    public long unreadCount(User u) {
        return db.notifications.stream().filter(n -> n.getUserId() == u.getId() && !n.isRead()).count();
    }

    public void markAllRead(User u) {
        db.notifications.stream().filter(n -> n.getUserId() == u.getId()).forEach(Notification::markRead);
        save();
    }

    // ---------- seed ----------
    public void seedIfEmpty() {
        if (!db.users.isEmpty()) return;
        register("Admin", "admin@helpdesk.com", "admin123", Role.ADMIN);
        User staff = register("Ravi Kumar", "staff@helpdesk.com", "staff123", Role.STAFF);
        User cust = register("Preethika", "preethika@helpdesk.com", "pass123", Role.CUSTOMER);

        Object[][] rows = {
            {"Feature Request", "Can you add a dark mode option?", Priority.LOW, Status.OPEN, "2025-02-02T09:20"},
            {"Password Reset", "I forgot my password, please help.", Priority.MEDIUM, Status.IN_PROGRESS, "2025-02-03T14:45"},
            {"Email Not Working", "I am not receiving college emails.", Priority.LOW, Status.RESOLVED, "2025-02-04T11:30"},
            {"Website Not Loading", "The college website is not opening.", Priority.MEDIUM, Status.IN_PROGRESS, "2025-02-05T15:15"},
            {"Login Issue", "I am not able to login to my account.", Priority.HIGH, Status.OPEN, "2025-02-06T10:24"},
        };
        for (Object[] r : rows) {
            Ticket t = new Ticket(db.nextTicketId++, (String) r[0], (String) r[1], (Priority) r[2],
                    cust.getId(), LocalDateTime.parse((String) r[4]));
            t.setStatus((Status) r[3]);
            if (t.getStatus() != Status.OPEN) t.setAssignedTo(staff.getId());
            db.tickets.add(t);
        }
        notify(cust.getId(), "Your ticket #5 has been updated.");
        notify(cust.getId(), "A new comment was added to ticket #3.");
        notify(cust.getId(), "Ticket #2 is now in progress.");
        save();
    }

    private void requireRole(User actor, Role role) {
        if (actor.getRole() != role) throw new SecurityException("You do not have permission for this action.");
    }
}
