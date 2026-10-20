package helpdesk;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/** Whole data set; persisted to a single file so the app keeps data between runs. */
public class Database implements Serializable {
    private static final long serialVersionUID = 1L;

    public final List<User> users = new ArrayList<>();
    public final List<Ticket> tickets = new ArrayList<>();
    public final List<Notification> notifications = new ArrayList<>();
    public int nextUserId = 1;
    public int nextTicketId = 1;
    public int nextNotificationId = 1;

    public static Database load(String path) {
        File f = new File(path);
        if (!f.exists()) return null;
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(f))) {
            return (Database) in.readObject();
        } catch (Exception e) {
            System.out.println("Could not read " + path + " (" + e.getMessage() + "). Starting fresh.");
            return null;
        }
    }

    public void save(String path) {
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(path))) {
            out.writeObject(this);
        } catch (IOException e) {
            System.out.println("Warning: could not save data: " + e.getMessage());
        }
    }
}
