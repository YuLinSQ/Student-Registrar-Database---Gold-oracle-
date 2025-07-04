import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;

public class StudentInterface {
    private Scanner scanner;
    private Connection conn;
    private String PERM;

    public StudentInterface(Connection conn) {
        this.conn = conn;
        this.scanner = new Scanner(System.in);
        System.out.println("Enter your PERM number: ");
        this.PERM = scanner.nextLine().trim();
        if (this.PERM.length() != 7 || !this.PERM.matches("\\d{7}")) {
            throw new IllegalArgumentException("Invalid PERM number");
        }
    }

    public void displayMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\nGOLD Interface");
            System.out.println("1. Add a course");
            System.out.println("2. Drop a course");
            System.out.println("3. View my current quarter schedule");
            System.out.println("4. View grades of a previous quarter");
            System.out.println("5. Run Requirements Checker");
            System.out.println("6. Make a plan");
            System.out.println("7. Change PIN");
            System.out.println("8. Exit");
            System.out.print("Select an option: ");

            String input = scanner.nextLine().trim();
            int choice = Integer.parseInt(input);
            switch (choice) {
                case 1:
                    addCourse();
                    break;
                case 2:
                    dropCourse();
                    break;
                case 3:
                    viewCurrentQuarterSchedule();
                    break;
                case 4:
                    viewGradesOfPreviousQuarter();
                    break;
                case 5:
                    runRequirementsChecker();
                    break;
                case 6:
                    makePlan();
                    break;
                case 7:
                    changePIN();
                    break;
                case 8:
                    System.out.println("Exiting Student Interface...");
                    running = false;
                    break;
            }
        }
    }


    private void addCourse() {
        System.out.println("Enter the course number to add (e.g. CS174): ");
        String courseNumber = scanner.nextLine().trim().toUpperCase();

        System.out.println("Enter the current quarter (e.g. 25S): ");
        String currentQuarter = scanner.nextLine().trim().toUpperCase();

        try (CallableStatement stmt = conn.prepareCall("{call ADDCOURSE(?, ?, ?)}")) {
            stmt.setString(1, PERM);
            stmt.setString(2, courseNumber);
            stmt.setString(3, currentQuarter);
            stmt.execute();
            System.out.println("Course added successfully.");
        } 
        catch (SQLException e) {
            String errorMessage = e.getMessage();
            // extracting only the user-friendly message, the first line of the error message after ORA-20002:
            if (errorMessage.contains("ORA-20002:")) {
                errorMessage = errorMessage.substring(errorMessage.indexOf("ORA-20002:") + 11);
                errorMessage = errorMessage.split("\n")[0];
            }
            System.err.println("Error: " + errorMessage.trim());
        }
    }


    private void dropCourse() {
        System.out.println("Enter the course number to drop (e.g. CS174): ");
        String courseNumber = scanner.nextLine().trim().toUpperCase();

        try (CallableStatement stmt = conn.prepareCall("{call DROPCOURSE(?, ?)}")) {
            stmt.setString(1, PERM);
            stmt.setString(2, courseNumber);
            stmt.execute();
            System.out.println("Course dropped successfully.");
        }
        catch (SQLException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("ORA-20002:")) {
                errorMessage = errorMessage.substring(errorMessage.indexOf("ORA-20002:") + 11);
                errorMessage = errorMessage.split("\n")[0];
            }
            System.err.println("Error: " + errorMessage.trim());
        }
    }


    private void viewCurrentQuarterSchedule() {
        System.out.println("Enter the current quarter (e.g. 25S): ");
        String currentQuarter = scanner.nextLine().trim().toUpperCase();
        String sql = """
                    SELECT e.CourseNumber, co.YearQuarterOffered, c.CourseTitle,
                           co.CourseOfferingslot, co.EnrollmentCode
                    FROM EnrolledIn e
                    JOIN CourseOfferings co ON e.CourseNumber = co.CourseNumber 
                        AND co.YearQuarterOffered = ?
                    JOIN Course c ON e.CourseNumber = c.CourseNumber 
                    WHERE e.PERM = ?""";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, currentQuarter);
            stmt.setString(2, PERM);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    System.out.println("\nNo courses found for entered quarter " + currentQuarter + ".\n");
                    return;
                }

                System.out.println("\nYour Current Quarter Schedule (" + currentQuarter + "):");
                System.out.println("----------------------------------------");
                System.out.printf("%-8s | %-30s | %-15s | %-8s%n", 
                    "Course", "Title", "Time", "Enrollment Code");
                System.out.println("----------------------------------------");

                while (rs.next()) {
                    System.out.printf("%-8s | %-30s | %-15s | %-8s%n",
                        rs.getString("CourseNumber"),
                        rs.getString("CourseTitle"),
                        rs.getString("CourseOfferingslot"),
                        rs.getString("EnrollmentCode"));
                }
                System.out.println("----------------------------------------\n");
            }
        }
        catch (SQLException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("ORA-20002:")) {
                errorMessage = errorMessage.substring(errorMessage.indexOf("ORA-20002:") + 11);
                errorMessage = errorMessage.split("\n")[0];
            }
            System.err.println("Error: " + errorMessage.trim()); 
        }
    }


    private void viewGradesOfPreviousQuarter() {
        System.out.println("Enter quarter you want to view grades for (e.g. 25W): ");
        String previousQuarter = scanner.nextLine().trim().toUpperCase();

        String sql = """
            SELECT c.CourseNumber, c.CourseTitle, co.Grade
            FROM Completed co
            JOIN Course c ON co.CourseNumber = c.CourseNumber
            WHERE co.PERM = ?
            AND co.YearQuarterOffered = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, PERM);
            stmt.setString(2, previousQuarter);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    System.out.println("\nNo grades found for entered quarter " + previousQuarter + ".\n");
                    return;
                }

                System.out.println("\nYour Grades for Quarter " + previousQuarter + ":");
                System.out.println("----------------------------------------");
                System.out.printf("%-8s | %-30s | %-15s%n", 
                    "Course", "Title", "Grade");
                System.out.println("----------------------------------------");

                while (rs.next()) {
                    System.out.printf("%-8s | %-30s | %-15s%n",
                        rs.getString("CourseNumber"),
                        rs.getString("CourseTitle"),
                        rs.getString("Grade"));
                }
                System.out.println("----------------------------------------\n");
            }
        }

        catch (SQLException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("ORA-20002:")) {
                errorMessage = errorMessage.substring(errorMessage.indexOf("ORA-20002:") + 11);
                errorMessage = errorMessage.split("\n")[0];
            }
            System.err.println("Error: " + errorMessage.trim()); 
        }
    }


    private void runRequirementsChecker() {
        System.out.println("Enter the current quarter (e.g. 25S): ");
        String currentQuarter = scanner.nextLine().trim().toUpperCase();

        try {
            // reference: used chatgpt to figure out how to enable DBMS_OUTPUT
            CallableStatement enableOutput = conn.prepareCall("BEGIN DBMS_OUTPUT.ENABLE(NULL); END;");
            enableOutput.execute();

            CallableStatement stmt = conn.prepareCall("{call CHECKREQUIREMENTS(?, ?)}");
            stmt.setString(1, PERM);
            stmt.setString(2, currentQuarter);
            stmt.execute();

            // REFERENCE: used chatgpt to figure out how to allocate a temporary buffer to read multiple lines
            CallableStatement getOutput = conn.prepareCall(
                "DECLARE " +
                "  l_line VARCHAR2(32767); " +
                "  l_status INTEGER; " +
                "BEGIN " +
                "  LOOP " +
                "    DBMS_OUTPUT.GET_LINE(l_line, l_status); " +
                "    EXIT WHEN l_status = 1; " +
                "    ? := l_line; " +
                "    EXIT; " +
                "  END LOOP; " +
                "END;"
            );

            getOutput.registerOutParameter(1, java.sql.Types.VARCHAR);

            System.out.println("\nRequirements Check Results:");
            System.out.println("-------------------------");

            // loop and re-execute for each line of output outlined in the procedure 
            while (true) {
                getOutput.execute();
                String line = getOutput.getString(1);
                if (line == null || line.trim().isEmpty()) break;
                System.out.println(line);
            }

            System.out.println("-------------------------\n");
        }
        
        catch (SQLException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("ORA-20002:")) {
                errorMessage = errorMessage.substring(errorMessage.indexOf("ORA-20002:") + 11);
                errorMessage = errorMessage.split("\n")[0];
            }
            System.err.println("Error: " + errorMessage.trim()); 
        }
    }


    private void makePlan() {
        System.out.println("Enter the current quarter (e.g. 25S): ");
        String currentQuarter = scanner.nextLine().trim().toUpperCase();

        try {
            // enable DBMS_OUTPUT, similar to requirements checker
            CallableStatement enableOutput = conn.prepareCall("BEGIN DBMS_OUTPUT.ENABLE(NULL); END;");
            enableOutput.execute();

            // stored procedure
            CallableStatement stmt = conn.prepareCall("{call MAKEPLAN(?, ?)}");
            stmt.setString(1, PERM);
            stmt.setString(2, currentQuarter);
            stmt.execute();

            // reading DBMS_OUTPUT, similar to requirements checker
            CallableStatement getOutput = conn.prepareCall(
                "DECLARE " +
                "  l_line VARCHAR2(32767); " +
                "  l_status INTEGER; " +
                "BEGIN " +
                "  LOOP " +
                "    DBMS_OUTPUT.GET_LINE(l_line, l_status); " +
                "    EXIT WHEN l_status = 1; " +
                "    ? := l_line; " +
                "    EXIT; " +
                "  END LOOP; " +
                "END;"
            );

            getOutput.registerOutParameter(1, java.sql.Types.VARCHAR);
            while (true) {
                getOutput.execute();
                String line = getOutput.getString(1);
                if (line == null || line.trim().isEmpty()) break;
                System.out.println(line);
            }
        }
        catch (SQLException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("ORA-20002:")) {
                errorMessage = errorMessage.substring(errorMessage.indexOf("ORA-20002:") + 11);
                errorMessage = errorMessage.split("\n")[0];
            }
            System.err.println("Error: " + errorMessage.trim()); 
        }
    }


    private void changePIN() {
        System.out.print("Enter your current PIN: ");
        String oldPIN = scanner.nextLine().trim();

        System.out.print("Enter the new PIN: ");
        String newPIN = scanner.nextLine().trim();

        if (!newPIN.matches("\\d{5}")) {
            System.err.println("Error: PIN must be 5 digits.");
            return;
        }

        try (CallableStatement stmt = conn.prepareCall("{call SetPin(?, ?, ?)}")) {
            stmt.setString(1, PERM);
            stmt.setString(2, oldPIN);
            stmt.setString(3, newPIN);
            stmt.execute();
            System.out.println("PIN changed successfully.");
        } 
        catch (SQLException e) {
            String msg = e.getMessage();
            if (msg.contains("ORA-20001")) {
                System.err.println("Error: Old PIN is incorrect.");
            } 
            else {
                System.err.println("Error: " + msg);
            }
        }
    }
} 