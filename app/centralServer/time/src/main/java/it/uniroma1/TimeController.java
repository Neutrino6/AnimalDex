package it.uniroma1;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.ArrayList;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
public class TimeController {

    private LocalDateTime deadline;
    private List<Integer> usersWithMaxPoints = new ArrayList<>();

    private static final String STORAGE_PATH = "/storage/deadline.txt";
    private static final String WINNERS_PATH = "/storage/winners.txt";

    public TimeController() throws ClientProtocolException, IOException {
        retrieveDeadlineFromStorage();
        if (deadline == null || deadline.isBefore(LocalDateTime.now())) {
            setNextMidnightDeadline();
            storeDeadlineToStorage();
            updateUsersWithMaxPoints();
        }
        loadWinnersFromStorage();
        scheduleMidnightTask();
    }

    private void setNextMidnightDeadline() {
        LocalDate today = LocalDate.now();
        LocalTime midnight = LocalTime.MIDNIGHT;
        deadline = LocalDateTime.of(today.plusDays(1), midnight);
    }

    private void scheduleMidnightTask() {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextMidnight = now.with(LocalTime.MIDNIGHT).plusDays(1);
        long initialDelay = now.until(nextMidnight, ChronoUnit.SECONDS);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                performMidnightTask();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }, initialDelay, 24 * 60 * 60, TimeUnit.SECONDS);
    }

    private void performMidnightTask() throws ClientProtocolException, IOException {
        System.out.println("Performing midnight task...");
        updateUsersWithMaxPoints();
        setNextMidnightDeadline();
        storeDeadlineToStorage();
    }

    private void updateUsersWithMaxPoints() throws ClientProtocolException, IOException {
        HttpPost request = new HttpPost("http://host.docker.internal:6039/getWinners");  //communication between different containers
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);

        
            System.out.println("Status code: " + statusCode);
            System.out.println("Response body: " + responseBody);

            //transform the response body into a list of integers
            String trimmed = responseBody.replaceAll("\\[|\\]|\\s", "");
            String[] stringNumbers = trimmed.split(",");

            for (String number : stringNumbers) {
                try {
                    usersWithMaxPoints.add(Integer.parseInt(number));
                } catch (NumberFormatException e) {
                    System.out.println("'" + number + "' is not a number.");
                }
            }
            
            storeWinnersToStorage();
        }
    }

    private void retrieveDeadlineFromStorage() {
        try {
            String content = new String(Files.readAllBytes(Paths.get(STORAGE_PATH)));
            deadline = LocalDateTime.parse(content);
        } catch (Exception e) {
            System.err.println("Error reading deadline from storage: " + e.getMessage());
            deadline = null;
        }
    }

    private void storeDeadlineToStorage() {
        try {
            Files.write(Paths.get(STORAGE_PATH), deadline.toString().getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (Exception e) {
            System.err.println("Error writing deadline to storage: " + e.getMessage());
        }
    }

    private void loadWinnersFromStorage() {
        try (BufferedReader reader = new BufferedReader(new FileReader(WINNERS_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int userId = Integer.parseInt(line);
                usersWithMaxPoints.add(userId);
            }
        } catch (IOException e) {
            System.err.println("Error reading deadline from storage: " + e.getMessage());
        }
    }

    private void storeWinnersToStorage() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(WINNERS_PATH))) {
            for (int userId : usersWithMaxPoints) {
                writer.write(userId + System.lineSeparator());
            }
        } catch (IOException e) {
            System.err.println("Error writing deadline to storage: " + e.getMessage());
        }
    }

    @RequestMapping("/getDeadline")
    public ResponseEntity<String> getDeadline() {
        return ResponseEntity.ok(deadline.toString());
    }

    @RequestMapping("/checkWinner")
    public ResponseEntity<String> checkWinner(@RequestParam("userId") int userId) {
        if (usersWithMaxPoints.contains(userId)) {
            return ResponseEntity.ok("User " + userId + " is a winner");
        } else {
            return ResponseEntity.ok("User " + userId + " is not a winner");
        }
    }
}