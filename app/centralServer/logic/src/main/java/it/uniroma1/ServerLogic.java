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

@RestController
public class ServerLogic {
    
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

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

}
