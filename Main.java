import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        Connection dbConnection = null;
        
        try {
            // database connection
            System.out.println("Initializing database connection...");
            dbConnection = DatabaseConnector.connect();
            System.out.println("Connected to the database!");
                        
            boolean running = true;
            while (running) {
                System.out.println("\n=== University Database System ===");
                System.out.println("1. Student Interface");
                System.out.println("2. Registrar Interface");
                System.out.println("3. Exit");
                System.out.print("Select an option: ");
                
                String choice = scanner.nextLine().trim();
                
                try {
                    switch (choice) {
                        case "1":
                            StudentInterface studentInterface = new StudentInterface(dbConnection);
                            studentInterface.displayMenu();
                            break;
                        case "2":
                            RegistrarInterface registrarInterface = new RegistrarInterface(dbConnection);
                            registrarInterface.displayMenu();
                            break;
                        case "3":
                            System.out.println("Exiting...");
                            running = false;
                            break;
                        default:
                            System.out.println("Invalid option. Please enter a number between 1 and 3.");
                    }
                } 
                
                catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                    System.out.println("Please try again.");
                }
            }
        } 
        
        catch (SQLException e) {
            System.err.println("Database connection error: " + e.getMessage());
            System.err.println("Please check your wallet files and database credentials.");
        } 
        
        catch (Exception e) {
            System.err.println("An unexpected error occurred: " + e.getMessage());
        } 
        
        finally {
            try {
                if (dbConnection != null && !dbConnection.isClosed()) {
                    dbConnection.close();
                    System.out.println("Database connection closed.");
                }
            } 
            
            catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
            
            scanner.close();
        }
    }
} 