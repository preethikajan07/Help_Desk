package helpdesk;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/** Terminal front end for the HelpDesk system. */
public class Main {
    private static final String DATA_FILE = "helpdesk.dat";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a");

    private final Scanner in = new Scanner(System.in);
    private final HelpDeskService svc;

    public Main(HelpDeskService svc) { this.svc = svc; }

    public static void main(String[] args) {
        Database db = Database.load(DATA_FILE);
        if (db == null) db = new Database();
        HelpDeskService svc = new HelpDeskService(db, DATA_FILE);
        svc.seedIfEmpty();
        new Main(svc).run();
    }

    // ================= entry =================
    private void run() {
        while (true) {
            System.out.println("\n==============================");
            System.out.println("   HELPDESK MANAGEMENT SYSTEM");
            System.out.println("==============================");
            System.out.println("1. Login");
            System.out.println("2. Register (customer)");
            System.out.println("0. Exit");
            switch (ask("Choose")) {
                case "1": login(); break;
                case "2": register(); break;
                case "0": System.out.println("Goodbye!"); return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private void register() {
        try {
            String name = ask("Name");
            String email = ask("Email");
            String pw = ask("Password (min 4 chars)");
            svc.register(name, email, pw, Role.CUSTOMER);
            System.out.println("Registered! You can log in now.");
        } catch (IllegalArgumentException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void login() {
        String email = ask("Email");
        String pw = ask("Password");
        User u = svc.login(email, pw).orElse(null);
        if (u == null) { System.out.println("Invalid email or password."); return; }
        System.out.println("\nWelcome, " + u.getName() + "!  (" + u.getRole() + ")");
        session(u);
    }

    // ================= session =================
    private void session(User u) {
        while (true) {
            long unread = svc.unreadCount(u);
            boolean customer = u.getRole() == Role.CUSTOMER;
            boolean admin = u.getRole() == Role.ADMIN;

            System.out.println("\n----------- MENU -----------");
            System.out.println("1. Dashboard");
            System.out.println("2. Create Ticket");
            System.out.println("3. My Tickets" + (u.getRole() == Role.STAFF ? " (assigned to me)" : ""));
            if (!customer) System.out.println("4. All Tickets");
            System.out.println("5. View / Update a Ticket");
            System.out.println("6. Search Tickets");
            System.out.println("7. Notifications" + (unread > 0 ? "  [" + unread + " new]" : ""));
            if (admin) System.out.println("8. Manage Users");
            System.out.println("0. Logout");

            String c = ask("Choose");
            try {
                switch (c) {
                    case "1": dashboard(u); break;
                    case "2": createTicket(u); break;
                    case "3": printTickets(customer ? svc.ticketsFor(u) : svc.assignedTo(u), u); break;
                    case "4":
                        if (customer) System.out.println("Invalid choice.");
                        else printTickets(svc.ticketsFor(u), u);
                        break;
                    case "5": viewTicket(u); break;
                    case "6": search(u); break;
                    case "7": notifications(u); break;
                    case "8":
                        if (admin) manageUsers(u); else System.out.println("Invalid choice.");
                        break;
                    case "0": System.out.println("Logged out."); return;
                    default: System.out.println("Invalid choice.");
                }
            } catch (IllegalArgumentException | SecurityException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // ================= screens =================
    private void dashboard(User u) {
        List<Ticket> mine = svc.ticketsFor(u);
        System.out.println("\nWelcome, " + u.getName() + "!");
        System.out.println("Here's an overview of your support requests.\n");
        for (Map.Entry<String, Long> e : svc.stats(mine).entrySet())
            System.out.printf("  %-14s : %d%n", e.getKey(), e.getValue());

        System.out.println("\nRecent tickets:");
        printTickets(mine.stream().limit(5).toList(), u);

        System.out.println("\nRecent notifications:");
        List<Notification> ns = svc.notificationsFor(u);
        if (ns.isEmpty()) System.out.println("  (none)");
        ns.stream().limit(4).forEach(n -> System.out.println("  " + (n.isRead() ? " " : "*") + " " + n.getMessage()));
    }

    private void createTicket(User u) {
        String title = ask("Title");
        String desc = ask("Description");
        Priority p = askPriority();
        Ticket t = svc.createTicket(u, title, desc, p);
        System.out.println("Ticket #" + t.getId() + " created.");
    }

    private void printTickets(List<Ticket> list, User viewer) {
        if (list.isEmpty()) { System.out.println("No tickets found."); return; }
        boolean showOwner = viewer.getRole() != Role.CUSTOMER;
        String line = "-".repeat(showOwner ? 132 : 112);
        System.out.println(line);
        System.out.printf("%-4s %-22s %-36s %-9s %-12s %-19s%s%n",
                "#", "Title", "Description", "Priority", "Status", "Created At", showOwner ? "  Customer / Staff" : "");
        System.out.println(line);
        for (Ticket t : list) {
            System.out.printf("%-4d %-22s %-36s %-9s %-12s %-19s%s%n",
                    t.getId(), cut(t.getTitle(), 22), cut(t.getDescription(), 36),
                    t.getPriority().label(), t.getStatus().label(), t.getCreatedAt().format(FMT),
                    showOwner ? "  " + svc.nameOf(t.getCreatedBy()) + " / " + svc.nameOf(t.getAssignedTo()) : "");
        }
        System.out.println(line);
        System.out.println("Showing " + list.size() + " ticket(s)");
    }

    private void viewTicket(User u) {
        int id = askInt("Ticket id");
        while (true) {
            Ticket t = svc.getTicket(u, id);
            System.out.println("\n========== Ticket #" + t.getId() + " ==========");
            System.out.println("Title       : " + t.getTitle());
            System.out.println("Description : " + t.getDescription());
            System.out.println("Priority    : " + t.getPriority().label());
            System.out.println("Status      : " + t.getStatus().label());
            System.out.println("Created by  : " + svc.nameOf(t.getCreatedBy()));
            System.out.println("Assigned to : " + svc.nameOf(t.getAssignedTo()));
            System.out.println("Created at  : " + t.getCreatedAt().format(FMT));
            System.out.println("Comments    :");
            if (t.getComments().isEmpty()) System.out.println("   (none)");
            for (Comment c : t.getComments())
                System.out.println("   [" + c.getCreatedAt().format(FMT) + "] " + svc.nameOf(c.getUserId()) + ": " + c.getText());

            System.out.println("\n a. Add comment");
            if (u.getRole() != Role.CUSTOMER) System.out.println(" s. Change status");
            if (u.getRole() == Role.ADMIN) {
                System.out.println(" g. Assign to staff");
                System.out.println(" d. Delete ticket");
            }
            System.out.println(" b. Back");
            String c = ask("Choose").toLowerCase();
            try {
                switch (c) {
                    case "a": svc.addComment(u, id, ask("Comment")); System.out.println("Comment added."); break;
                    case "s": svc.updateStatus(u, id, askStatus()); System.out.println("Status updated."); break;
                    case "g":
                        for (User s : svc.staffMembers()) System.out.println("   " + s.getId() + " - " + s.getName());
                        svc.assign(u, id, askInt("Staff id")); System.out.println("Ticket assigned."); break;
                    case "d":
                        if (ask("Type YES to confirm delete").equals("YES")) {
                            svc.deleteTicket(u, id); System.out.println("Ticket deleted."); return;
                        }
                        break;
                    case "b": return;
                    default: System.out.println("Invalid choice.");
                }
            } catch (IllegalArgumentException | SecurityException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    private void search(User u) {
        System.out.println("1. Search by keyword   2. Filter by status");
        String c = ask("Choose");
        if (c.equals("1")) printTickets(svc.search(u, ask("Keyword")), u);
        else if (c.equals("2")) printTickets(svc.filterByStatus(u, askStatus()), u);
        else System.out.println("Invalid choice.");
    }

    private void notifications(User u) {
        List<Notification> ns = svc.notificationsFor(u);
        if (ns.isEmpty()) System.out.println("No notifications.");
        for (Notification n : ns)
            System.out.println((n.isRead() ? "   " : " * ") + "[" + n.getCreatedAt().format(FMT) + "] " + n.getMessage());
        svc.markAllRead(u);
    }

    private void manageUsers(User admin) {
        while (true) {
            System.out.println("\n--- Manage Users ---");
            System.out.println("1. List users");
            System.out.println("2. Add staff member");
            System.out.println("3. Add admin");
            System.out.println("4. Delete user");
            System.out.println("0. Back");
            String c = ask("Choose");
            try {
                switch (c) {
                    case "1":
                        System.out.printf("%-4s %-22s %-30s %s%n", "ID", "Name", "Email", "Role");
                        for (User x : svc.allUsers())
                            System.out.printf("%-4d %-22s %-30s %s%n", x.getId(), cut(x.getName(), 22), x.getEmail(), x.getRole());
                        break;
                    case "2": svc.register(ask("Name"), ask("Email"), ask("Password"), Role.STAFF); System.out.println("Staff added."); break;
                    case "3": svc.register(ask("Name"), ask("Email"), ask("Password"), Role.ADMIN); System.out.println("Admin added."); break;
                    case "4": svc.deleteUser(admin, askInt("User id")); System.out.println("User deleted."); break;
                    case "0": return;
                    default: System.out.println("Invalid choice.");
                }
            } catch (IllegalArgumentException | SecurityException e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // ================= input helpers =================
    private String ask(String prompt) {
        System.out.print(prompt + ": ");
        if (!in.hasNextLine()) { System.out.println(); System.exit(0); }
        return in.nextLine().trim();
    }

    private int askInt(String prompt) {
        try { return Integer.parseInt(ask(prompt)); }
        catch (NumberFormatException e) { throw new IllegalArgumentException("Please enter a number."); }
    }

    private Priority askPriority() {
        while (true) {
            String s = ask("Priority (1=Low, 2=Medium, 3=High)");
            switch (s) {
                case "1": return Priority.LOW;
                case "2": return Priority.MEDIUM;
                case "3": return Priority.HIGH;
                default: System.out.println("Enter 1, 2 or 3.");
            }
        }
    }

    private Status askStatus() {
        while (true) {
            String s = ask("Status (1=Open, 2=In Progress, 3=Resolved, 4=Closed)");
            switch (s) {
                case "1": return Status.OPEN;
                case "2": return Status.IN_PROGRESS;
                case "3": return Status.RESOLVED;
                case "4": return Status.CLOSED;
                default: System.out.println("Enter 1-4.");
            }
        }
    }

    private static String cut(String s, int n) {
        return s.length() <= n ? s : s.substring(0, n - 3) + "...";
    }
}
