// ScheduledCommandUtility.java
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScheduledCommandUtility {

    private static final String FILE_PATH = "tmp/commands.txt";
    private static final String ONE_TIME_PATTERN = "^(\\d{1,2})\\s+(\\d{1,2})\\s+(\\d{1,2})\\s+(\\d{1,2})\\s+(\\d{4})\\s+(.*)";
    private static final String RECURRING_PATTERN = "^\\*/(\\d+)\\s+(.*)";
    private static final int[] ALLOWED_INTERVALS = { 1, 2, 3, 4, 5, 6, 10, 12, 15, 20, 30, 60 };

    public static void main(String[] args) {
        System.out.println("Starting Scheduled Command Utility...");

        List<String> commands = loadCommandsFromFile(FILE_PATH);
        if (commands.isEmpty()) {
            System.out.println("No commands found in " + FILE_PATH);
            return;
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

        for (String line : commands) {
            parseAndScheduleCommand(line, scheduler);
        }
    }

    private static List<String> loadCommandsFromFile(String filePath) {
        List<String> commands = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    commands.add(line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
        return commands;
    }

    private static void parseAndScheduleCommand(String line, ScheduledExecutorService scheduler) {
        Pattern oneTimeRegex = Pattern.compile(ONE_TIME_PATTERN);
        Matcher oneTimeMatcher = oneTimeRegex.matcher(line);

        Pattern recurringRegex = Pattern.compile(RECURRING_PATTERN);
        Matcher recurringMatcher = recurringRegex.matcher(line);

        if (oneTimeMatcher.matches()) {
            scheduleOneTimeCommand(oneTimeMatcher, scheduler);
        } else if (recurringMatcher.matches()) {
            scheduleRecurringCommand(recurringMatcher, scheduler);
        } else {
            System.err.println("Invalid command format: " + line);
        }
    }

    private static void scheduleOneTimeCommand(Matcher matcher, ScheduledExecutorService scheduler) {
        try {
            int minute = Integer.parseInt(matcher.group(1));
            int hour = Integer.parseInt(matcher.group(2));
            int day = Integer.parseInt(matcher.group(3));
            int month = Integer.parseInt(matcher.group(4));
            int year = Integer.parseInt(matcher.group(5));
            String command = matcher.group(6);

            LocalDateTime scheduledTime = LocalDateTime.of(year, month, day, hour, minute);
            LocalDateTime now = LocalDateTime.now();

            long delay = java.time.Duration.between(now, scheduledTime).toSeconds();

            if (delay > 0) {
                System.out.println("Scheduling one-time command '" + command + "' for "
                        + scheduledTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                scheduler.schedule(() -> executeCommand(command), delay, TimeUnit.SECONDS);
            } else {
                System.err.println("One-time command is in the past, skipping: " + matcher.group(0));
            }
        } catch (Exception e) {
            System.err.println("One-time command is invalid: " + matcher.group(0));
        }
    }

    private static boolean isAllowedInterval(int n) {
        for (int allowed : ALLOWED_INTERVALS) {
            if (allowed == n)
                return true;
        }
        return false;
    }

    private static void scheduleRecurringCommand(Matcher matcher, ScheduledExecutorService scheduler) {
        int minutes = Integer.parseInt(matcher.group(1));
        String command = matcher.group(2);

        if (!isAllowedInterval(minutes)) {
            System.err.println(
                    "Invalid recurring interval: " + minutes + " minutes. Allowed: 1,2,3,4,5,6,10,12,15,20,30,60");
            return;
        }

        System.out.println("Scheduling recurring command '" + command + "' to run every " + minutes + " minutes.");
        scheduler.scheduleAtFixedRate(() -> executeCommand(command), 0, minutes, TimeUnit.MINUTES);
    }

    private static ProcessBuilder getProcessBuilder(String command) {
        String cmd = System.getProperty("os.name").toLowerCase();
        if (cmd.contains("win")) {
            // Replace '&&' with '^&' for Windows and escape double quotes
            String winCommand = command.replaceAll("\\bdate\\b", "echo %DATE% %TIME%");
            return new ProcessBuilder("cmd.exe", "/c", winCommand);
        } else if (cmd.contains("mac")) {
            return new ProcessBuilder("zsh", "-c", command);
        } else {
            return new ProcessBuilder("bash", "-c", command);
        }
    }

    private static void executeCommand(String command) {
        try {
            System.out.println("Executing command: " + command);
            ProcessBuilder pb = getProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read and print the output
            try (BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(process.getInputStream()));
                    // Append output to a file
                    java.io.FileWriter fw = new java.io.FileWriter("sample-output.txt", true);
                    java.io.BufferedWriter bw = new java.io.BufferedWriter(fw)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                    bw.write(line);
                    bw.newLine();
                }
            }

            int exitCode = process.waitFor();
            System.out.println("Command '" + command + "' exited with code " + exitCode);
        } catch (IOException | InterruptedException e) {
            System.err.println("Error executing command: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
