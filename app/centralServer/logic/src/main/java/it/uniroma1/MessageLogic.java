package it.uniroma1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.io.IOException;
import java.util.List;

@RestController
public class MessageLogic {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @RequestMapping(value = "/sendMessage", method = RequestMethod.POST)
    public ResponseEntity<String> sendMessage(@RequestBody String messageRequest) throws JsonProcessingException, IOException {
        int u_id=0;
        String o_id="",writer="",text="";
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(messageRequest);
        if (jsonNode.has("u_id")) {
            u_id = jsonNode.get("u_id").asInt();
        }
        if (jsonNode.has("o_id")) {
            o_id = jsonNode.get("o_id").asText();
        }
        if (jsonNode.has("writer")) {
            writer = jsonNode.get("writer").asText();
        }
        if (jsonNode.has("text")) {
            text = jsonNode.get("text").asText();
        }

        // SQL query to insert the message into the "message" table
        String sql = "INSERT INTO message (u_id, o_id, writer, text) " +
                    "VALUES (:u_id, :o_id, :writer, :text)";

        // Creating a MapSqlParameterSource to hold the parameters
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("u_id", u_id);
        params.addValue("o_id", o_id);
        params.addValue("writer", writer);
        params.addValue("text", text);

        try {
            // Execute the SQL update
            jdbcTemplate.update(sql, params);
            // Return success response
            return ResponseEntity.status(HttpStatus.CREATED).body("Message sent successfully!");
        } catch (Exception e) {
            // Handle any exceptions and return an error response
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error sending message: " + e.getMessage());
        }
    }

    
    @RequestMapping(value = "/getMessages", method = RequestMethod.GET)
    public ResponseEntity<?> getMessages(
        @RequestParam(value = "u_id", required = false) Integer u_id,
        @RequestParam(value = "o_id", required = false) String o_id) {

        if (u_id == null && o_id == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("At least one of 'u_id' or 'o_id' must be provided.");
        }

        String sql = "SELECT * FROM message WHERE 1=1";
        MapSqlParameterSource params = new MapSqlParameterSource();

        if (u_id != null) {
            sql += " AND u_id = :u_id";
            params.addValue("u_id", u_id);
        }

        if (o_id != null && !o_id.equals("")) {
            sql += " AND o_id = :o_id";
            params.addValue("o_id", o_id);
        }
        sql += " ORDER BY c_date;";
        try {
            List<Map<String, Object>> messages = jdbcTemplate.queryForList(sql, params);
            return ResponseEntity.ok(messages);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error retrieving messages: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/deleteMessages", method = RequestMethod.DELETE)
    public ResponseEntity<String> deleteMessages(
        @RequestParam(value = "u_id") Integer u_id,
        @RequestParam(value = "o_id") String o_id) {

        String sql = "DELETE FROM message WHERE u_id = :u_id AND o_id = :o_id";
        
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("u_id", u_id);
        params.addValue("o_id", o_id);

        try {
            int rowsAffected = jdbcTemplate.update(sql, params);
            if (rowsAffected > 0) {
                return ResponseEntity.ok("Message deleted successfully!");
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Message not found.");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error deleting message: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/saveRating", method = RequestMethod.POST)
    public ResponseEntity<String> saveRating(@RequestBody String messageRequest) throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(messageRequest);
        int u_id=0; 
        int rating=0;
        String o_id="";
        if (jsonNode.has("user_id")) {
            u_id = jsonNode.get("user_id").asInt();
        }
        if (jsonNode.has("operator_id")) {
            o_id = jsonNode.get("operator_id").asText();
        }
        if (jsonNode.has("rating")) {
            rating = jsonNode.get("rating").asInt();
        }

        String sql = "Select count(*) FROM ranking WHERE u_id = :u_id AND o_id = :o_id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("u_id", u_id);
        params.addValue("o_id", o_id);
        try {
            Integer count = jdbcTemplate.queryForObject(sql, params,Integer.class);
            if (count == 0) {
                String sql2 = "Insert into ranking (u_id, o_id, eval) values (:u_id, :o_id, :eval)";
                MapSqlParameterSource params2 = new MapSqlParameterSource();
                params2.addValue("u_id", u_id);
                params2.addValue("o_id", o_id);
                params2.addValue("eval", rating);
                try {
                    jdbcTemplate.update(sql2, params2);
                    return ResponseEntity.ok("{\"message\":\"evaluation correctly inserted\"}");
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\":\"error inserting evaluation\"}");
                }
            } else {
                String sql3 = "Update ranking set eval=:eval where u_id = :u_id and o_id = :o_id";
                MapSqlParameterSource params3 = new MapSqlParameterSource();
                params3.addValue("u_id", u_id);
                params3.addValue("o_id", o_id);
                params3.addValue("eval", rating);
                try {
                    jdbcTemplate.update(sql3, params3);
                    return ResponseEntity.ok("{\"message\":\"evaluation correctly updated\"}");
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\":\"error inserting evaluation\"}");
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\\\"message\\\":\\\"Error while inserting message: \" + e.getMessage() + \"\\\"}");
        }
    }

    @RequestMapping(value = "/saveRatingOperator", method = RequestMethod.POST)
    public ResponseEntity<String> saveRatingOperator(@RequestBody String messageRequest) throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(messageRequest);
        int u_id=0; 
        int rating=0;
        String o_id="";
        if (jsonNode.has("user_id")) {
            u_id = jsonNode.get("user_id").asInt();
        }
        if (jsonNode.has("operator_id")) {
            o_id = jsonNode.get("operator_id").asText();
        }
        if (jsonNode.has("rating")) {
            rating = jsonNode.get("rating").asInt();
        }

        String sql = "Select count(*) FROM ranking WHERE u_id = :u_id AND o_id = :o_id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("u_id", u_id);
        params.addValue("o_id", o_id);
        try {
            Integer count = jdbcTemplate.queryForObject(sql, params,Integer.class);
            if (count == 0) {
                String sql2 = "Insert into ranking (u_id, o_id, eval_op) values (:u_id, :o_id, :eval)";
                MapSqlParameterSource params2 = new MapSqlParameterSource();
                params2.addValue("u_id", u_id);
                params2.addValue("o_id", o_id);
                params2.addValue("eval", rating);
                try {
                    jdbcTemplate.update(sql2, params2);
                    return ResponseEntity.ok("{\"message\":\"evaluation correctly inserted\"}");
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\":\"error inserting evaluation\"}");
                }
            } else {
                String sql3 = "Update ranking set eval_user=:eval where u_id = :u_id and o_id = :o_id";
                MapSqlParameterSource params3 = new MapSqlParameterSource();
                params3.addValue("u_id", u_id);
                params3.addValue("o_id", o_id);
                params3.addValue("eval", rating);
                try {
                    jdbcTemplate.update(sql3, params3);
                    return ResponseEntity.ok("{\"message\":\"evaluation correctly updated\"}");
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"message\":\"error inserting evaluation\"}");
                }
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\\\"message\\\":\\\"Error while inserting message: \" + e.getMessage() + \"\\\"}");
        }
    }


    @RequestMapping(value = "/getRating", method = RequestMethod.POST)
    public ResponseEntity<?> getRating(@RequestBody String messageRequest) throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(messageRequest);
        String o_id="";
        if (jsonNode.has("id")) {
            o_id = jsonNode.get("id").asText();
        }
        String sql = "Select eval FROM ranking WHERE o_id = :o_id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("o_id", o_id);
        try {
            List<Integer> ratings = jdbcTemplate.queryForList(sql, params, Integer.class);
            if (ratings.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No ratings found for the given ID");
            }
            double averageRating = ratings.stream()
                                          .mapToInt(Integer::intValue)
                                          .average()
                                          .orElse(0.0);

            // Restituisce la media in formato JSON
            return ResponseEntity.ok(averageRating);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error to calculate the average: " + e.getMessage());
        }
    }

    @RequestMapping(value = "/getRatingUser", method = RequestMethod.POST)
    public ResponseEntity<?> getRatingUser(@RequestBody String messageRequest) throws JsonProcessingException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(messageRequest);
        Integer u_id=0;
        if (jsonNode.has("id")) {
            u_id = jsonNode.get("id").asInt();
        }
        String sql = "Select eval_user FROM ranking WHERE u_id = :u_id";
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("u_id", u_id);
        try {
            List<Integer> ratings = jdbcTemplate.queryForList(sql, params, Integer.class);
            if (ratings.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No ratings found for the given ID");
            }
            else if(ratings == null){
                double averageRating = 0;
                return ResponseEntity.ok(averageRating);
            }
            else{
                double averageRating = ratings.stream()
                                          .mapToInt(Integer::intValue)
                                          .average()
                                          .orElse(0.0);

                // Restituisce la media in formato JSON
                return ResponseEntity.ok(averageRating);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error to calculate the average: " + e.getMessage());
        }
    }
}
