package com.example;

import java.sql.*;
import java.util.Arrays;
import java.util.Scanner;
import java.util.regex.Pattern;

public class Main {

    private final Scanner scanner = new Scanner(System.in);

    static void main(String[] args) {
        if (isDevMode(args)) {
            DevDatabaseInitializer.start();
        }
        new Main().run();
    }

    public void run() {
        // Resolve DB settings with precedence: System properties -> Environment variables
        String jdbcUrl = resolveConfig("APP_JDBC_URL", "APP_JDBC_URL");
        String dbUser = resolveConfig("APP_DB_USER", "APP_DB_USER");
        String dbPass = resolveConfig("APP_DB_PASS", "APP_DB_PASS");

        if (jdbcUrl == null || dbUser == null || dbPass == null) {
            throw new IllegalStateException(
                    "Missing DB configuration. Provide APP_JDBC_URL, APP_DB_USER, APP_DB_PASS " +
                            "as system properties (-Dkey=value) or environment variables.");
        }

        try (Connection connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPass)) {

                boolean authorized = login(connection);


                if (!authorized) {
                    System.out.println("Invalid username or password");
                    System.out.println("press 0 to exit");
                    while(true){
                        String exit = scanner.nextLine().trim();
                        if(exit.equals("0")){
                            return;
                        }
                    }
                }


            while(true) {
                int option = promptMenu();

                switch (option) {
                    case 1 -> listMissions(connection);
                    case 2 -> getMission(connection);
                    case 3 -> missionsCountYear(connection);
                    case 4 -> createAccount(connection);
                    case 5 -> updatePassword(connection);
                    case 6 -> deleteAccount(connection);
                    case 0 -> {
                        return;
                    }

                    default -> System.out.println("Invalid choice.\n");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Prompts username and password, checks if the combination is present in accounts
     *
     * @param connection
     * @return <code>true</code> if the name/password combo exists
     * <code>false</code> if either name/password isn't present
     */
    private boolean login(Connection connection) {
        System.out.println("Username: ");
        String unm = scanner.nextLine();
        System.out.println("Password: ");
        String pw = scanner.nextLine();

        //Query count if a row has the username and password combo, binary ensures case sensitivity
        String accQuery = "select count(*) from account where binary name = ? and binary password = ?";
        try(PreparedStatement statement = connection.prepareStatement(accQuery)){
            statement.setString(1, unm);
            statement.setString(2, pw);

            ResultSet rs = statement.executeQuery();

            return rs.next() && rs.getInt("count(*)") == 1; //if the credentials are found count will return 1


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prompts the menu options and returns a user input integer
     * @return integer >= 0
     * @see #getValidInt(String)
     */
    private int promptMenu(){
        System.out.print("\n" +
                "1) List moon missions (prints spacecraft names from `moon_mission`).\n" +
                "2) Get a moon mission by mission_id (prints details for that mission).\n" +
                "3) Count missions for a given year (prompts: year; prints the number of missions launched that year).\n" +
                "4) Create an account (prompts: first name, last name, ssn, password; prints confirmation).\n" +
                "5) Update an account password (prompts: user_id, new password; prints confirmation).\n" +
                "6) Delete an account (prompts: user_id; prints confirmation).\n" +
                "0) Exit.\n");

        return getValidInt("Enter Choice: ");
    }


    /**
     * Lists all spacecraft from the moon_mission table
     * @param connection
     */
    private void listMissions(Connection connection){
        String spaceshipQuery = "select spacecraft from moon_mission";
        try(PreparedStatement statement = connection.prepareStatement(spaceshipQuery)){

            ResultSet rs = statement.executeQuery();
            while(rs.next()) {
                System.out.println(rs.getString("spacecraft"));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * Prompts for a mission ID and prints its data is available
     * @param connection
     * @see #getValidInt(String)
     */
    private void getMission(Connection connection){
        int id = getValidInt("Mission Id: ");

        String  missionQuery = "select * from moon_mission where mission_id = ?";
        try(PreparedStatement statement = connection.prepareStatement(missionQuery)){
            statement.setInt(1, id);

            ResultSet rs = statement.executeQuery();

            if(rs.next()) {
                System.out.println(
                        "\nSpacecraft: " + rs.getString("spacecraft") +
                        "\nLaunch date: " + rs.getString("launch_date") +
                        "\nCarrier rocket: " + rs.getString("carrier_rocket") +
                        "\nOperator: " + rs.getString("operator") +
                        "\nMission type: " + rs.getString("mission_type") +
                        "\nOutcome: " + rs.getString("outcome"));
            }
            else  {
                System.out.println("\nMission not found.");
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prompts for a year and prints how many rows in moon_mission was launched then
     * @param connection
     * @see #getValidInt(String)
     */
    private void missionsCountYear(Connection connection){
        String missionYearQuery = "select count(*) from moon_mission where year(launch_date) = ?";
        int year = getValidInt("Mission Year: ");

        try(PreparedStatement statement = connection.prepareStatement(missionYearQuery)){
            statement.setInt(1, year);

            ResultSet rs = statement.executeQuery();

            while(rs.next()) {
                System.out.println("\nMissions in " + year + ": " + rs.getInt("count(*)"));
            }

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    /**
     * Gives a flow for creating  a new account, asking for First and Last name, SSN and password
     * default accountname is assigned if available and promted for if not
     * @param connection
     * @see #getValidName(String)
     * @see #getValidSSN(String)
     * @see #getValidPassword(String)
     */
    private void createAccount(Connection connection) {
        String fn = getValidName("First Name: ");
        String ln = getValidName("Last Name: ");
        String ssn = getValidSSN("SSN: ");
        String pw = getValidPassword("Password: ");

        String accName; //Default accountname is first three letters of first and last name
        if(fn.length() <= 3 && ln.length() <=3){    //if both first and last name are 3 or fewer letters
            accName = fn + ln;}                     //accountname is both full combined

        else if (fn.length() <= 3){                 //if only first name is 3 or fewer letters
            accName = fn + ln.substring(0, 2);      //accountname is full first name and first 3 from last name
        }
        else if(ln.length() <= 3){                  //if only last name is 3 or fewer letters
           accName = fn.substring(0, 2) + ln;       //accountname is first 3 from first name and full last name
        }
        else{                                       //if both are longer than 3 accountname follows default pattern
            accName = fn.substring(0, 2) + ln.substring(0, 2);
        }

        //Query to check if the accountname exists
        String checkName = "select count(*) from account where name = ?";

        while(true) {
            try (PreparedStatement statement = connection.prepareStatement(checkName)) {
                statement.setString(1, accName);

                ResultSet rs = statement.executeQuery();


                if(rs.next() && rs.getInt("count(*)") == 0){ //if accountname is available continue
                    break;
                }
                else{
                    accName = getValidName("Account Name: "); //if not prompt for a new accountname and check again
                    //todo add help method for accountname
                }


            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        //Query to add account, ID is auto assigned
        String newAccQuery = "INSERT INTO account (name, password, first_name, last_name, ssn) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(newAccQuery)) {
            statement.setString(1, accName);
            statement.setString(2, pw);
            statement.setString(3, fn);
            statement.setString(4, ln);
            statement.setString(5, ssn);

            statement.executeUpdate();

            //todo check if new account is present
            System.out.println("\nAccount created");


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }


    /**
     * Updates password after prompting for an ID and new password
     * @param connection
     * @see #getValidInt(String)
     * @see #getValidPassword(String)
     */
    private void updatePassword (Connection connection) {
        int id = getValidInt("User id: "); //todo add check if account is present
        String newPassword = getValidPassword("New password: ");

        String updatePwQuery = "update account set password = ? where user_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(updatePwQuery)) {
            statement.setString(1, newPassword);
            statement.setInt(2, id);

            statement.executeUpdate();

            //todo add check if updated
            System.out.println("updated");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }


    }

    /**
     * Deletes account after prompting for an ID
     * @param connection
     * @see #getValidInt(String)
     */
    private void deleteAccount(Connection connection) {
        int id = getValidInt("User id: "); //todo add check if account is present

        String deleteAccQuery = "delete from account where user_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(deleteAccQuery)) {
            statement.setInt(1, id);

            statement.executeUpdate();

            //todo add check if deleted
            System.out.println("deleted");


        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Help method to get a valid and positive integer from input
     * @param prompt message to explain what is asked
     * @return integer >= 0
     */
    private int getValidInt(String prompt){
        while(true){
            try {
                System.out.println("\n" + prompt);
                int option = Integer.parseInt(scanner.nextLine());

                if (option >= 0) {
                    return option;
                }
                else  {
                    System.out.println("Please enter a positive integer.\n");
                }
            }
            catch (NumberFormatException e){
                System.out.println("Please enter a valid integer\n");
            }
        }
    }

    /**
     * Help method to get a valid string that only contains letters from input
     * @param prompt message to explain what is asked
     * @return Capitalized string with only letters
     */
    private String getValidName(String prompt){
        while(true){
            System.out.println("\n" + prompt);
            String name = scanner.nextLine().trim();

            if (name.isBlank()) {
                System.out.println("\nCannot be blank");
            }
            else if(!Pattern.matches("^[a-zA-Z]+$", name)){
                System.out.println("\nMust only contain letters");
            }
            else{
                return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
            }
        }
    }

    /**
     * Help method to get a correctly formated SSN from input
     * @param prompt message to explain what is asked
     * @return String with YYMMDD-XXXX formatting
     */
    private String getValidSSN(String prompt){
        while(true){
            System.out.println("\n" + prompt);
            String ssn = scanner.nextLine().trim();

            if (ssn.isBlank()) {
                System.out.println("\nCannot be blank");
            }
            else if(!Pattern.matches("^\\d{6}-\\d{4}$", ssn)){
                System.out.println("\nMust follow pattern YYMMDD-XXXX");
            }
            else {
                return ssn;
            }
        }
    }

    /**
     * Help method to get a valid password from input
     * @param prompt message to explain what is asked
     * @return string of at least 6 characters
     */
    private String getValidPassword(String prompt){
        while(true){
            System.out.println("\n" + prompt);
            String pw = scanner.nextLine();

            if(pw.length() < 6){
                System.out.println("Password must be at least 6 characters");
            }
            else{
                return pw;
            }
        }
    }


    /**
     * Determines if the application is running in development mode based on system properties,
     * environment variables, or command-line arguments.
     *
     * @param args an array of command-line arguments
     * @return {@code true} if the application is in development mode; {@code false} otherwise
     */
    private static boolean isDevMode(String[] args) {
        if (Boolean.getBoolean("devMode"))  //Add VM option -DdevMode=true
            return true;
        if ("true".equalsIgnoreCase(System.getenv("DEV_MODE")))  //Environment variable DEV_MODE=true
            return true;
        return Arrays.asList(args).contains("--dev"); //Argument --dev
    }

    /**
     * Reads configuration with precedence: Java system property first, then environment variable.
     * Returns trimmed value or null if neither source provides a non-empty value.
     */
    private static String resolveConfig(String propertyKey, String envKey) {
        String v = System.getProperty(propertyKey);
        if (v == null || v.trim().isEmpty()) {
            v = System.getenv(envKey);
        }
        return (v == null || v.trim().isEmpty()) ? null : v.trim();
    }
}
