package com.example;

import com.repo.AccountRepository;
import com.repo.DTO.AccountDTO;
import com.repo.DTO.MissionDTO;
import com.repo.MoonMissionRepository;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
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

        //Setup datasource and repositories
        DataSource ds = new SimpleDriverManagerDataSource(jdbcUrl, dbUser, dbPass);

        AccountRepository ac = new AccountRepository(ds);
        MoonMissionRepository mmc = new MoonMissionRepository(ds);

        //Try logging in
        boolean authorized = login(ac);

        //For tests, we don't care if it fails
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

        /**Main loop**/
        while(true) {
            int option = promptMenu();

            switch (option) {
                case 1 -> listMissions(mmc);
                case 2 -> getMission(mmc);
                case 3 -> missionsCountYear(mmc);
                case 4 -> createAccount(ac);
                case 5 -> updatePassword(ac);
                case 6 -> deleteAccount(ac);
                case 0 -> {
                    return;
                }

                default -> System.out.println("Invalid choice.\n");
            }
        }



    }

    /**
     * Prompts username and password, checks if the combination is present in accounts
     *
     * @return <code>true</code> if the name/password combo exists
     * <code>false</code> if either name/password isn't present
     */
    private boolean login(AccountRepository ac) {
        System.out.println("Username: ");
        String unm = scanner.nextLine();
        System.out.println("Password: "); //DB has pw in plaintext so we do too
        String pw = scanner.nextLine();

        return ac.matchCredentials(unm, pw);
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
     */
    private void listMissions(MoonMissionRepository mmc){
        List<MissionDTO> missions = mmc.getAllMissions();

        for(MissionDTO mission : missions){
            System.out.println(mission.spacecraft());
        }
    }


    /**
     * Prompts for a mission ID and prints its data is available
     * @see #getValidInt(String)
     */
    private void getMission(MoonMissionRepository mmc){
        int id = getValidInt("Mission Id: ");

        Optional<MissionDTO> mission = mmc.getMissionById(id);
        if(mission.isPresent()) {
            MissionDTO m =  mission.get();
            System.out.println(
                    "\nSpacecraft: " + m.spacecraft() +
                    "\nLaunch date: " + m.launchDate() +
                    "\nCarrier rocket: " + m.carrierRocket() +
                    "\nOperator: " + m.operator() +
                    "\nMission type: " + m.missionType() +
                    "\nOutcome: " + m.outcome());
        }
        else  {
            System.out.println("\nMission not found.");
        }
    }

    /**
     * Prompts for a year and prints how many rows in moon_mission was launched then
     * @see #getValidInt(String)
     */
    private void missionsCountYear(MoonMissionRepository mmc){
        int year = getValidInt("Mission Year: ");

        System.out.println("\nMissions in " + year + ": " + mmc.missionCount(year));

    }

    /**
     * Gives a flow for creating  a new account, asking for First and Last name, SSN and password
     * default accountname is assigned if available and promted for if not
     * @see #getValidName(String)
     * @see #getValidSSN(String)
     * @see #getValidPassword(String)
     */
    private void createAccount(AccountRepository ac) {
        String fn = getValidName("First Name: ");
        String ln = getValidName("Last Name: ");
        String ssn = getValidSSN("SSN: ");
        String pw = getValidPassword("Password: ");

        //Default accountname is first three letters of first and last name
        //If one or both are shorter we use what's available
        int fnN = Math.min(3, fn.length());
        int lnN = Math.min(3, ln.length());
        String accName = fn.substring(0, fnN) + ln.substring(0, lnN);

        //Check if the accountname exists
        Optional<AccountDTO> nameCheck;

        while(true) {
            nameCheck = ac.getAccountByName(accName);

            if(nameCheck.isEmpty()){ //if accountname is available continue
                    break;
                }
                else{
                    accName = getValidUsername("Account Name: "); //if not prompt for a new accountname and check again
                }
        }

        //Create and add account
        AccountDTO newAccount = new AccountDTO(0, accName, pw, fn, ln, ssn);

        ac.createAccount(newAccount);

        //check that account is present
        if(ac.accountExists(accName)){
            System.out.println("\nAccount created");
        }
        else{
            System.out.println("\nAccount creation failed.");
        }

    }


    /**
     * Updates password after prompting for an ID and new password
     * @see #getValidInt(String)
     * @see #getValidPassword(String)
     */
    private void updatePassword (AccountRepository ac) {
        int id = getValidInt("User id: ");

        if(ac.accountExists(id)){
            String newPassword = getValidPassword("New password: ");

            ac.updatePassword(id, newPassword);

            //confirm that the new password has been added
            if(ac.accountExists(id) && ac.matchCredentials(ac.getAccountByID(id).get().name(), newPassword)){
                System.out.println("Password updated");
            }
            else {
                System.out.println("Password update failed.");
            }
        }
        else{
            System.out.println("Account not found.");
        }

    }

    /**
     * Deletes account after prompting for an ID
     * @see #getValidInt(String)
     */
    private void deleteAccount(AccountRepository ac) {
        int id = getValidInt("User id: ");

        if(ac.accountExists(id)){
            ac.deleteAccount(id);
            if(!ac.accountExists(id)){
                System.out.println("Account deleted");
            }
            else {
                System.out.println("Account deletion failed.");
            }
        }
        else {
            System.out.println("Account not found.");
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
        //semi robust ssn pattern
        Pattern SSN_PATTERN = Pattern.compile(
                        "^(\\d{2})" +                  // YY
                        "(0[1-9]|1[0-2])" +            // MM (01-12)
                        "(0[1-9]|[12]\\d|3[01])" +     // DD (01-31)
                        "-\\d{4}"                      // Last 4
        );

        while(true){
            System.out.println("\n" + prompt);
            String ssn = scanner.nextLine().trim();

            if (ssn.isBlank()) {
                System.out.println("\nCannot be blank");
            }
            else if(!SSN_PATTERN.matcher(ssn).matches()){
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

            if(pw.length() < 6){ //6 letters as an arbitrary constraint
                System.out.println("Password must be at least 6 characters");
            }
            else{
                return pw;
            }
        }
    }

    /**
     * Help method to get a valid username from input
     * @param prompt message to explain what is asked
     * @return nonblank string
     */
    private String getValidUsername(String prompt){
        while(true){
            System.out.println("\n" + prompt);
            String un = scanner.nextLine().trim();

            if(un.isEmpty()){ //No real relevant constraint for this level of application
                System.out.println("Invalid username");
            }
            else{
                return un;
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
