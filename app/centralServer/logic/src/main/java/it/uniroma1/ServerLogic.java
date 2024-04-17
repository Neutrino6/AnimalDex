package it.uniroma1;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

import javax.sql.DataSource;
import javax.validation.constraints.Null;

@RestController
public class ServerLogic {
    
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @RequestMapping("/hello")
    public String hello(){
        return "Hello this server is running";
    }

    @RequestMapping(value = "/newCertificate", method = RequestMethod.POST)
    public ResponseEntity<String> newCertificate(@RequestBody String requestBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(requestBody);
        
            if (jsonNode.has("animalName")) {
                String animalName = jsonNode.get("animalName").asText();
                if (!animalName.isEmpty()) {
                    MapSqlParameterSource source1 = new MapSqlParameterSource()
                        .addValue("animalName",animalName);
                    String sql = "SELECT a_id FROM animal WHERE :animalName ILIKE concat('%',a_name,'%')";
                    Integer animalId = jdbcTemplate.queryForObject(sql, source1, Integer.class);
                    if(animalId!=null){
                        String insertCertificate= "INSERT INTO certification (animal_id, user_id, cert_date) VALUES (:a_id,:u_id,:time);";
                        String checkCertificate= "SELECT count(*) from certification where animal_id = :a_id and user_id = :u_id";
                        String updateCertificate= "UPDATE certification SET cert_date = :time where animal_id = :a_id and user_id = :u_id";
                        String discoveryAnimal= "Select concat('regions: ',regions,' ; ','details: ', details) from animal where a_id = :a_id";
                        Integer exampleUserId=new Integer(999);
                        LocalDateTime currentTime = LocalDateTime.now();
                        MapSqlParameterSource source2 = new MapSqlParameterSource()
                            .addValue("a_id",animalId)
                            .addValue("u_id", exampleUserId)
                            .addValue("time", currentTime);
                        MapSqlParameterSource source3 = new MapSqlParameterSource()
                            .addValue("a_id",animalId)
                            .addValue("u_id", exampleUserId);
                        String response;

                        // check whether the user has already a certificate regarding animal with id = animalId
                        Integer count = jdbcTemplate.queryForObject(checkCertificate, source3,Integer.class);
                        if(count>0 && count!=null){
                            //update the last certificate
                            jdbcTemplate.update(updateCertificate, source2);
                            response="Certicate updated, animal_id = ";
                            response=response+animalId;
                        }
                        else {
                            MapSqlParameterSource source4 = new MapSqlParameterSource()
                                .addValue("a_id",animalId);

                            //insert a new certificate
                            jdbcTemplate.update(insertCertificate, source2);
                            response="Certicate inserted, animal_id = ";
                            response=response+animalId+"; ";

                            response=response+jdbcTemplate.queryForObject(discoveryAnimal, source4,String.class);
                        }
                        return ResponseEntity.ok(String.valueOf(response));
                    }
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("animalName not found");
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Void value for animalName");
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("animalName not found");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during JSON parsing.");
        } catch (EmptyResultDataAccessException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Animal not found");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        }
    }

//requestparam

    @RequestMapping(value = "/userSignUp", method = RequestMethod.POST)
    public ResponseEntity<String> userSignUp(@RequestBody String requestBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(requestBody);

            if (jsonNode.has("email") && jsonNode.has("password") && jsonNode.has("username")) {
                String email = jsonNode.get("email").asText();
                String hashedPassw = jsonNode.get("password").asText();
                String username = jsonNode.get("username").asText();
                if (!email.isEmpty() && !hashedPassw.isEmpty() && !username.isEmpty()) {
                    MapSqlParameterSource source1 = new MapSqlParameterSource()
                            .addValue("u_email", email);
                    String checkEmail = "SELECT count(*) from users where email = :u_email";
                    Integer count = jdbcTemplate.queryForObject(checkEmail, source1, Integer.class);
                    if (count != null && count > 0) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email already used by someone");
                    } else {
                        String firstname = null;
                        String surname = null;
                        LocalDateTime dob = null;
                        if (jsonNode.has("name")) {
                            firstname = jsonNode.get("name").asText();
                        }
                        if (jsonNode.has("surname")) {
                            surname = jsonNode.get("surname").asText();
                        }
                        if (jsonNode.has("dateofbirth")) {
                            String dobString = jsonNode.get("dateofbirth").asText();
                            dobString+="T00:00:00";
                            // Parse date from string
                            dob = LocalDateTime.parse(dobString);
                        }
                        // Save user information in the database
                        MapSqlParameterSource source2 = new MapSqlParameterSource()
                            .addValue("u_email", email)
                            .addValue("hpassw", hashedPassw)
                            .addValue("username", username)
                            .addValue("firstname", firstname)
                            .addValue("surname", surname)
                            .addValue("birthday", dob);
                        String insertQuery = "INSERT INTO users (email, passw, username, firstname, surname, birthday) VALUES (:u_email, :hpassw, :username, :firstname, :surname, :birthday)";
                        jdbcTemplate.update(insertQuery, source2);
                        return ResponseEntity.status(HttpStatus.CREATED).body("User signed up successfully");
                    }
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Void value for email, hashed password, or username");
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email, hashed password, or username not found in the request");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during JSON parsing.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        }
    }

    @RequestMapping(value = "/userSignIn", method = RequestMethod.POST)
    public ResponseEntity<String> userSignIn(@RequestBody String requestBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(requestBody);
            if (jsonNode.has("email") && jsonNode.has("hashedPassw")) {
                String email = jsonNode.get("email").asText();
                String hashedPassw = jsonNode.get("hashedPassw").asText();
                MapSqlParameterSource source1 = new MapSqlParameterSource()
                    .addValue("u_email", email)
                    .addValue("hpassw", hashedPassw);
                String insertQuery = "select count(*) from users where email = :u_email and passw = :hpassw";
                if(jdbcTemplate.queryForObject(insertQuery, source1, Integer.class) > 0){
                    return ResponseEntity.status(HttpStatus.OK).body("User signed in successfully");
                }
                else return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Incorrect login credential");
            }
            else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Email or hashed password not found in the request");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error during JSON parsing.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        }
    }
}
