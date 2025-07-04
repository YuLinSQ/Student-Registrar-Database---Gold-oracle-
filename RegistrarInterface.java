import java.sql.Connection;
import java.sql.SQLException;
import java.util.Scanner;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.CallableStatement;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class RegistrarInterface {
    private Scanner scanner;
    private Connection conn;

    public RegistrarInterface(Connection conn) {
        this.conn = conn;
        this.scanner = new Scanner(System.in);
    }

    public void displayMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\nRegistrar Interface");
            System.out.println("1. Add a student to a course");
            System.out.println("2. Drop a student from a course");
            System.out.println("3. List courses currently taken by a student");
            System.out.println("4. List grades of previous quarter for a student");
            System.out.println("5. Generate a class list for a course");
            System.out.println("6. Enter grades for a course for all students");
            System.out.println("7. Request a transcript for a student");
            System.out.println("8. Generate a grade mailer for all students");
            System.out.println("9. Exit");
            System.out.print("Select an option: ");

            String input = scanner.nextLine().trim();
            int choice = Integer.parseInt(input);

        switch (choice) {
            case 1:
                addStudentToCourse();
                break;
            case 2:
                dropStudentFromCourse();
                break;
            case 3:
                listCoursesCurrentlyTakenByStudent();
                break;
            case 4:
                listGradesOfPreviousQuarter();
                break;
            case 5:
                generateClassListForCourse();
                break;
            case 6:
                enterGradesForCourseForAllStudents();
                break;
            case 7:
                requestTranscriptForStudent();
                break;
            case 8:
                generateGradeMailerForAllStudents();
                break;
            case 9:
                System.out.println("Exiting Registrar Interface...");
                running = false;
                break;
            }
        }
    }

    private void addStudentToCourse() {
        System.out.println("Enter the student's PERM (7 digits): ");
        String PERM = scanner.nextLine().trim();
        if (PERM.length() != 7 || !PERM.matches("\\d{7}")) {
            throw new IllegalArgumentException("Invalid PERM number");
        }

        System.out.println("Enter the course number to add the student to (e.g. CS174): ");
        String courseNumber = scanner.nextLine().trim().toUpperCase();

        System.out.println("Enter the current quarter (e.g. 25S): ");
        String currentQuarter = scanner.nextLine().trim().toUpperCase();

        try (CallableStatement stmt = conn.prepareCall("{call ADDCOURSE(?, ?, ?)}")) {
            stmt.setString(1, PERM);
            stmt.setString(2, courseNumber);
            stmt.setString(3, currentQuarter);
            stmt.execute();
            System.out.println("Student added to course successfully.");
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

    private void dropStudentFromCourse() {
        System.out.println("Enter the student's PERM (7 digits): ");
        String PERM = scanner.nextLine().trim();
        if (PERM.length() != 7 || !PERM.matches("\\d{7}")) {
            throw new IllegalArgumentException("Invalid PERM number");
        }

        System.out.println("Enter the course number to drop the student from (e.g. CS174): ");
        String courseNumber = scanner.nextLine().trim().toUpperCase();

        try (CallableStatement stmt = conn.prepareCall("{call DROPCOURSE(?, ?)}")) {
            stmt.setString(1, PERM);
            stmt.setString(2, courseNumber);
            stmt.execute();
            System.out.println("Student dropped from course successfully.");
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
    
    private void listCoursesCurrentlyTakenByStudent() {
        System.out.println("Enter the student's PERM (7 digits):");
        String PERM = scanner.nextLine().trim();
        if (PERM.length() != 7 || !PERM.matches("\\d{7}")) {
            throw new IllegalArgumentException("Invalid PERM number");
        }

        String sql = "SELECT CourseNumber FROM EnrolledIn WHERE PERM = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, PERM);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    System.out.println("\nNo courses found for student with PERM: " + PERM + ".\n");
                    return;
                }

                System.out.println("\nCourses currently taken by student with PERM: " + PERM + ":");
                System.out.println("----------------------------------------");
                while (rs.next()) {
                    System.out.printf("%-8s%n",
                        rs.getString("CourseNumber"));
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

    private void listGradesOfPreviousQuarter() {
        System.out.println("Enter the student's PERM (7 digits): ");
        String PERM = scanner.nextLine().trim();
        if (PERM.length() != 7 || !PERM.matches("\\d{7}")) {
            throw new IllegalArgumentException("Invalid PERM number");
        }

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
                    System.out.println("\nNo grades found for student with PERM: " + PERM + " in quarter " + previousQuarter + ".\n");
                    return;
                }

                System.out.println("\nGrades for student with PERM: " + PERM + " in quarter " + previousQuarter + ":");
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
    
    private void generateClassListForCourse() {
        System.out.println("Enter the course number to generate a class list for (e.g. CS174): ");
        String courseNumber = scanner.nextLine().trim().toUpperCase();
    
        String sql = """
            SELECT e.PERM, s.NAME
            FROM EnrolledIn e
            JOIN Student s ON e.PERM = s.PERM
            WHERE e.CourseNumber = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    System.out.println("\nNo students enrolled in course: " + courseNumber + ".\n");
                    return;
                }

                System.out.println("\nStudents enrolled in course: " + courseNumber + ":");
                System.out.println("----------------------------------------");
                System.out.printf("%-8s | %-30s%n", 
                    "PERM", "NAME"); 
                System.out.println("----------------------------------------");
                while (rs.next()) {
                    System.out.printf("%-8s | %-30s%n",
                        rs.getString("PERM"),
                        rs.getString("NAME"));
                }
                System.out.println("----------------------------------------");
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

    /* 
    - 
    The CSV file should have the following format:
    Course Code,Course Quarter,Perm,Grade
        - Course Code is enrollment code
        - Course Quarter is the quarter the course was taken in (e.g. 25S)
        - Perm is the student's PERM
        - Grade is the grade the student received
    
    The function reads the CSV file and enter the grades into the Completed table.

    The function will also delete the student from the EnrolledIn table.

    This should happen at the end of the quarter, effectively making the course "completed" 
    and removing the student from the course.
    - 
    */
    private void enterGradesForCourseForAllStudents() {
        System.out.println("Please enter file name of .csv file (make sure file exists in \"grades\" folder): ");
        String filename = scanner.nextLine().trim();
        String filePath = "grades/" + filename;
        List<String[]> gradeEntries = new ArrayList<>();

        // read the CSV file
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty() || line.startsWith("Course")) continue; // headers
                String[] parts = line.split(",");
                if (parts.length != 4) {
                    System.err.println("Invalid line in CSV: " + line);
                    continue;
                }
                gradeEntries.add(parts);
            }
        } 
        catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            return;
        }

        // current quarter once
        System.out.print("Enter the current quarter (e.g. 25S - note: You cannot enter grades for the current quarter): ");
        String currentQuarter = scanner.nextLine().trim().toUpperCase();

        // Process each entry
        for (String[] entry : gradeEntries) {
            String enrollmentCode = entry[0].trim().toUpperCase();
            String quarter = entry[1].trim().toUpperCase();
            String perm = entry[2].trim();
            String grade = entry[3].trim().toUpperCase();

            // cannot enter grade if its the current quarter
            if (quarter.equals(currentQuarter)) {
                System.out.println("Cannot enter grades for current quarter: " + currentQuarter);
                return;
            }

            // first, get course number and yearquarteroffered from enrollment code
            String sql = """
                SELECT CourseNumber, YearQuarterOffered
                FROM CourseOfferings
                WHERE EnrollmentCode = ?
            """;
            String courseNumber = null;
            String yearQuarterOffered = null;
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, enrollmentCode);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    courseNumber = rs.getString("CourseNumber");
                    yearQuarterOffered = rs.getString("YearQuarterOffered");
                } else {
                    System.err.println("No course found for enrollment code: " + enrollmentCode);
                    continue;
                }
            }
            catch (SQLException e) {
                System.err.println("Error getting course number and year/quarter offered from enrollment code: " + e.getMessage());
                continue;
            }

            try {
                conn.setAutoCommit(false);

                try (
                    PreparedStatement insertStmt = conn.prepareStatement("""
                        INSERT INTO Completed (PERM, CourseNumber, YearQuarterOffered, Grade)
                        VALUES (?, ?, ?, ?)
                    """);
                    PreparedStatement deleteStmt = conn.prepareStatement("""
                        DELETE FROM EnrolledIn
                        WHERE PERM = ? AND EnrollmentCode = ?
                    """)) {

                    insertStmt.setString(1, perm);
                    insertStmt.setString(2, courseNumber);
                    insertStmt.setString(3, yearQuarterOffered);
                    insertStmt.setString(4, grade);
                    insertStmt.executeUpdate();

                    deleteStmt.setString(1, perm);
                    deleteStmt.setString(2, enrollmentCode);
                    deleteStmt.executeUpdate();
                    
                    conn.commit();
                    System.out.println("Processed: " + perm + " | " + courseNumber + " | " + yearQuarterOffered + " | " + grade);
                }
            } 
            catch (SQLException e) {
                try {
                    // REFERENCE: chatgpt for rollback and auto commit functionality 
                    conn.rollback();
                } 
                catch (SQLException rollbackEx) {
                    System.err.println("Error rolling back transaction: " + rollbackEx.getMessage());
                }
                System.err.println("Error processing PERM " + perm + ": " + e.getMessage());
            } 
            finally {
                try {
                    conn.setAutoCommit(true);
                } 
                catch (SQLException e) {
                    System.err.println("Error resetting auto-commit: " + e.getMessage());
                }
            }
        }
        System.out.println("Grade entry complete.");
    }
    

    private void requestTranscriptForStudent() {
        System.out.println("Enter the student's PERM (7 digits): ");
        String PERM = scanner.nextLine().trim();
        if (PERM.length() != 7 || !PERM.matches("\\d{7}")) {
            throw new IllegalArgumentException("Invalid PERM number");
        }

        String sql = """
            SELECT CourseNumber, Grade, YearQuarterOffered
            FROM Completed
            WHERE PERM = ?
            ORDER BY 
                SUBSTR(YearQuarterOffered, 1, 2) DESC,
                CASE SUBSTR(YearQuarterOffered, 3, 1)
                    WHEN 'W' THEN 1
                    WHEN 'F' THEN 2
                    WHEN 'S' THEN 3
                END
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, PERM);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.isBeforeFirst()) {
                    System.out.println("\nNo transcript found for student with PERM: " + PERM + ".\n");
                    return;
                }

                System.out.println("\nTranscript for student with PERM: " + PERM + ":");
                System.out.println("----------------------------------------");
                System.out.printf("%-8s | %-8s | %-8s%n", 
                    "Course", "Grade", "Quarter"); 
                System.out.println("----------------------------------------");
                while (rs.next()) {
                    System.out.printf("%-8s | %-8s | %-8s%n", 
                        rs.getString("CourseNumber"),
                        rs.getString("Grade"),
                        rs.getString("YearQuarterOffered"));
                }
                System.out.println("----------------------------------------");
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

    private void generateGradeMailerForAllStudents() {
        System.out.println("Enter Course to Generate Grade Mailer For (e.g. CS174): ");
        String courseNumber = scanner.nextLine().trim().toUpperCase();

        System.out.println("Enter the quarter to generate grade mailer for (has to be a previous quarter, e.g. 25W): ");
        String quarter = scanner.nextLine().trim().toUpperCase();

        String sql = """
            SELECT s.NAME, s.PERM, c.CourseNumber, c.CourseTitle, co.Grade
            FROM Student s
            JOIN Completed co ON s.PERM = co.PERM
            JOIN Course c ON co.CourseNumber = c.CourseNumber
            WHERE co.CourseNumber = ? AND co.YearQuarterOffered = ?
            ORDER BY s.NAME, c.CourseNumber
        """;

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, courseNumber);
            stmt.setString(2, quarter);

            try (ResultSet rs = stmt.executeQuery()) {
                boolean hasResults = false;
                String currentStudent = "";
                String currentPERM = "";
                String courseTitle = "";

                while (rs.next()) {
                    String studentName = rs.getString("NAME");
                    String studentPERM = rs.getString("PERM");
                    courseTitle = rs.getString("CourseTitle");

                    // If this is a new student, print the header
                    if (!studentName.equals(currentStudent)) {
                        if (hasResults) {
                            // Print footer for previous student
                            System.out.println("==========================================\n");
                        }
                        
                        currentStudent = studentName;
                        currentPERM = studentPERM;
                        hasResults = true;

                        // Print header for new student
                        System.out.println("==========================================");
                        System.out.println("GRADE MAILER - " + courseNumber + " - " + courseTitle);
                        System.out.println("==========================================");
                        System.out.println("Student Name: " + studentName);
                        System.out.println("PERM: " + studentPERM);
                        System.out.println("Quarter: " + quarter);
                        System.out.println("----------------------------------------");
                        System.out.println("Grade: " + rs.getString("Grade"));
                    }
                }

                if (!hasResults) {
                    System.out.println("No grades found for course " + courseNumber + " in quarter " + quarter + ".");
                } else {
                    // Print final footer
                    System.out.println("==========================================\n");
                }
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
} 