import java.sql.*;
import java.util.Scanner;

public class ToDoListApp {
    private static Connection conn;
    private static int currentUserId;

    public static void main(String[] args) {
        try {
            // Initialize SQLite connection
            conn = DriverManager.getConnection("jdbc:sqlite:todo_list.db");
            createTables();

            Scanner scanner = new Scanner(System.in);
            while (true) {
                System.out.println("\n1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Choose an option: ");
                int choice = scanner.nextInt();
                scanner.nextLine(); // Consume newline

                if (choice == 1) {
                    registerUser(scanner);
                } else if (choice == 2) {
                    if (loginUser(scanner)) {
                        taskMenu(scanner);
                    }
                } else if (choice == 3) {
                    System.out.println("Goodbye!");
                    break;
                } else {
                    System.out.println("Invalid choice. Try again.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
        }
    }

    private static void createTables() throws SQLException {
        Statement stmt = conn.createStatement();
        // Create user table
        stmt.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY, username TEXT UNIQUE, password TEXT)");
        // Create task table
        stmt.execute("CREATE TABLE IF NOT EXISTS tasks (id INTEGER PRIMARY KEY, user_id INTEGER, description TEXT, priority TEXT, completed BOOLEAN, FOREIGN KEY(user_id) REFERENCES users(id))");
    }

    private static void registerUser(Scanner scanner) throws SQLException {
        System.out.print("Enter a username: ");
        String username = scanner.nextLine();
        System.out.print("Enter a password: ");
        String password = scanner.nextLine();

        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            pstmt.executeUpdate();
            System.out.println("Registration successful!");
        } catch (SQLException e) {
            System.out.println("Error: Username already exists.");
        }
    }

    private static boolean loginUser(Scanner scanner) throws SQLException {
        System.out.print("Enter your username: ");
        String username = scanner.nextLine();
        System.out.print("Enter your password: ");
        String password = scanner.nextLine();

        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    currentUserId = rs.getInt("id");
                    System.out.println("Login successful!");
                    return true;
                } else {
                    System.out.println("Invalid credentials.");
                    return false;
                }
            }
        }
    }

    private static void taskMenu(Scanner scanner) throws SQLException {
        while (true) {
            System.out.println("\n1. Add Task");
            System.out.println("2. View Tasks");
            System.out.println("3. Mark Task as Completed");
            System.out.println("4. Delete Task");
            System.out.println("5. Logout");
            System.out.print("Choose an option: ");
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    addTask(scanner);
                    break;
                case 2:
                    viewTasks();
                    break;
                case 3:
                    markTaskAsCompleted(scanner);
                    break;
                case 4:
                    deleteTask(scanner);
                    break;
                case 5:
                    System.out.println("Logging out...");
                    currentUserId = 0;
                    return;
                default:
                    System.out.println("Invalid choice. Try again.");
            }
        }
    }

    private static void addTask(Scanner scanner) throws SQLException {
        System.out.print("Enter task description: ");
        String description = scanner.nextLine();
        System.out.print("Enter task priority (high, medium, low): ");
        String priority = scanner.nextLine();

        String sql = "INSERT INTO tasks (user_id, description, priority, completed) VALUES (?, ?, ?, 0)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            pstmt.setString(2, description);
            pstmt.setString(3, priority);
            pstmt.executeUpdate();
            System.out.println("Task added successfully.");
        }
    }

    private static void viewTasks() throws SQLException {
        String sql = "SELECT id, description, priority, completed FROM tasks WHERE user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                System.out.println("\nYour Tasks:");
                while (rs.next()) {
                    String status = rs.getBoolean("completed") ? "Completed" : "Pending";
                    System.out.printf("%d. %s (Priority: %s, Status: %s)\n", rs.getInt("id"), rs.getString("description"), rs.getString("priority"), status);
                }
            }
        }
    }

    private static void markTaskAsCompleted(Scanner scanner) throws SQLException {
        viewTasks();
        System.out.print("Enter the task ID to mark as completed: ");
        int taskId = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        String sql = "UPDATE tasks SET completed = 1 WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            pstmt.setInt(2, currentUserId);
            int rowsUpdated = pstmt.executeUpdate();
            if (rowsUpdated > 0) {
                System.out.println("Task marked as completed.");
            } else {
                System.out.println("Invalid task ID.");
            }
        }
    }

    private static void deleteTask(Scanner scanner) throws SQLException {
        viewTasks();
        System.out.print("Enter the task ID to delete: ");
        int taskId = scanner.nextInt();
        scanner.nextLine(); // Consume newline

        String sql = "DELETE FROM tasks WHERE id = ? AND user_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            pstmt.setInt(2, currentUserId);
            int rowsDeleted = pstmt.executeUpdate();
            if (rowsDeleted > 0) {
                System.out.println("Task deleted.");
            } else {
                System.out.println("Invalid task ID.");
            }
        }
    }
}
