# HelpDesk Management System (Terminal Backend, Java)

Requires JDK 17+ (no libraries, no database setup).

## Run
    Linux/Mac:  ./run.sh
    Windows:    run.bat
    Manual:     javac -d out src/helpdesk/*.java && java -cp out helpdesk.Main

Data is saved automatically to `helpdesk.dat` (delete it to reset to sample data).

## Demo logins
| Role     | Email                   | Password |
|----------|-------------------------|----------|
| Customer | preethika@helpdesk.com  | pass123  |
| Staff    | staff@helpdesk.com      | staff123 |
| Admin    | admin@helpdesk.com      | admin123 |

## Features (mirrors the UI)
- Dashboard: Total / Open / In Progress / Resolved, recent tickets, recent notifications
- Create Ticket (title, description, priority), My Tickets, All Tickets (staff/admin)
- View ticket, comments, search by keyword, filter by status
- Staff/Admin: change status. Admin: assign to staff, delete tickets, manage users
- Notifications on create / status change / assignment / comments (unread counter)
- Role-based access: customers only see their own tickets; passwords stored as SHA-256 hashes

## Structure
    model:   User, Ticket, Comment, Notification, Role, Priority, Status
    storage: Database (file persistence)
    logic:   HelpDeskService
    UI:      Main (console menus)
