package it.uniroma1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.time.LocalDateTime;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;


@RestController
public class CertificateLogic {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private HttpServletResponse response;

    @RequestMapping(value = "/newCertificate", method = RequestMethod.POST)
    public ResponseEntity<String> newCertificate(@RequestParam("user_id") int user_id,@RequestBody String requestBody) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(requestBody);

            //Integer exampleUserId=new Integer(999);
        
            if (jsonNode.has("animalName")) {
                String animalName = jsonNode.get("animalName").asText();
                if (!animalName.isEmpty()) {
                    return certificateManagement(animalName,user_id);
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

    @RequestMapping(value = "/getScoreBoard")
    public ResponseEntity<String> getScoreBoard() {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode responseJson = mapper.createObjectNode();

        String getScoreBoard = "select u.username, u.email, u.points, a.a_name from users u left join animal a on a.a_id=u.fav_animal where user_id!=999 order by points desc;";
        MapSqlParameterSource params=new MapSqlParameterSource();
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(getScoreBoard,params);

        ArrayNode usersArray = mapper.createArrayNode();
        for (Map<String, Object> result : rows) {
            String username = (String) result.get("username");
            String email = (String) result.get("email");
            int points = (int) result.get("points");
            String favAnimal = (String) result.get("fav_animal");

            ObjectNode userNode = mapper.createObjectNode();
            userNode.put("Username", username);
            userNode.put("Email", email);
            userNode.put("Points", points);
            if(favAnimal==null){
                userNode.put("Favorite animal", "");
            }
            else userNode.put("Favorite animal", favAnimal);

            usersArray.add(userNode);
        }

        responseJson.set("users", usersArray);

        try {
            String jsonResponse = mapper.writeValueAsString(responseJson);
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            // Handle exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while processing the request.");
        }
    }

    @RequestMapping("getWinners")
    public ResponseEntity<List<Integer>> getWinners(){
        String sql = "SELECT user_id FROM users WHERE points = (SELECT MAX(points) FROM users where user_id!=999) and user_id!=999";
        List<Integer> updatedUsersWithMaxPoints = jdbcTemplate.queryForList(sql, new MapSqlParameterSource(), Integer.class);
        return ResponseEntity.ok(updatedUsersWithMaxPoints);
    }

    @Transactional
    private ResponseEntity<String> certificateManagement(String animalName,int user_id) throws JsonProcessingException{

        ObjectMapper mapper = new ObjectMapper();

        MapSqlParameterSource source1 = new MapSqlParameterSource()
            .addValue("animalName",animalName);
        String sql = "SELECT a_id FROM animal WHERE :animalName ILIKE concat('%',a_name,'%')";
        Integer animalId = jdbcTemplate.queryForObject(sql, source1, Integer.class);
        if(animalId!=null){
            String insertCertificate= "INSERT INTO certification (animal_id, user_id, cert_date) VALUES (:a_id,:u_id,:time);";
            String checkCertificate= "SELECT count(*) from certification where animal_id = :a_id and user_id = :u_id";
            String updateCertificate= "UPDATE certification SET cert_date = :time where animal_id = :a_id and user_id = :u_id";
            String discoveryAnimal= "Select regions,details from animal where a_id = :a_id";
            
            LocalDateTime currentTime = LocalDateTime.now();
            MapSqlParameterSource source2 = new MapSqlParameterSource()
                .addValue("a_id",animalId)
                .addValue("u_id", user_id)
                .addValue("time", currentTime);
            MapSqlParameterSource source3 = new MapSqlParameterSource()
                .addValue("a_id",animalId)
                .addValue("u_id", user_id);
            
            ObjectNode responseJson = mapper.createObjectNode();

            // check whether the user has already a certificate regarding animal with id = animalId
            Integer count = jdbcTemplate.queryForObject(checkCertificate, source3,Integer.class);
            if(count>0 && count!=null){
                //update the last certificate
                jdbcTemplate.update(updateCertificate, source2);
                responseJson.put("message", "Certificate updated");
                responseJson.put("animal_id", animalId);
            }
            else {
                MapSqlParameterSource source4 = new MapSqlParameterSource()
                    .addValue("a_id",animalId);

                //insert a new certificate
                jdbcTemplate.update(insertCertificate, source2);
                responseJson.put("message", "Certificate inserted");
                responseJson.put("animal_id", animalId);

                List<Map<String, Object>> rows =jdbcTemplate.queryForList(discoveryAnimal, source4);
                if (!rows.isEmpty()) {
                    Map<String, Object> result = rows.get(0);
                    String regions = (String) result.get("regions");
                    String details = (String) result.get("details");
                    responseJson.put("regions", regions);
                    responseJson.put("details", details);
                }
            }
            updatePoints(user_id,animalId,count);
            String jsonResponse = mapper.writeValueAsString(responseJson);
            return ResponseEntity.ok(jsonResponse);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("animalName not found");
    }

    // this method updates the points get by an user
    // isInserted is a variable which has value 0 if this is the first time that user user_id inserts an certificates
    // regarding animal a_id, otherwise it's > 0
    private void updatePoints(int user_id, int a_id, int isInserted){
        if(isInserted==0){
            String getPoints= "Select std_points from animal where a_id = :a_id";
            String updateUser= "update users set points = points + :animal_points where user_id = :user_id";
            MapSqlParameterSource source1 = new MapSqlParameterSource()
                .addValue("a_id", a_id);
            Integer animal_points = jdbcTemplate.queryForObject(getPoints, source1, Integer.class);
            MapSqlParameterSource source2 = new MapSqlParameterSource()
                .addValue("animal_points", animal_points)
                .addValue("user_id", user_id);
            jdbcTemplate.update(updateUser, source2);
        }
        return;
    }

    /*StartDate date not null,
    EndDate date not null,
    BonusPoints int not null,
    animal_id int not null,*/
    @RequestMapping(value = "/newSpecialEvent", method = RequestMethod.POST)
    public ModelAndView newSpecialEvent(@RequestParam("user_id") int user_id,@RequestParam("end_date") String end_date,
        @RequestParam("bonus_points") int bonus_points, @RequestParam("animal_name") String animal_name) {
        
        /*
         * check if the user is authorized
         */
        LocalDateTime start_date=LocalDateTime.now();
        String getAnimalIdByName="select a_id from animal where a_name = :a_name";
        MapSqlParameterSource source1 = new MapSqlParameterSource().addValue("a_name", animal_name);
        int animal_id=jdbcTemplate.queryForObject(getAnimalIdByName, source1, Integer.class);

        MapSqlParameterSource source2 = new MapSqlParameterSource()
            .addValue("animal_id", animal_id)
            .addValue("now", start_date);
        String checkEvents = "select true from SpecialEvent where animal_id = :animal_id and StartDate<= :now and :now<EndDate";
        try{
            Boolean condition = jdbcTemplate.queryForObject(checkEvents, source2, Boolean.class);
        } catch (EmptyResultDataAccessException e) {
            end_date+="T00:00:00";
            
            MapSqlParameterSource source3 = new MapSqlParameterSource()
                .addValue("animal_id", animal_id)
                .addValue("now", start_date)
                .addValue("end", LocalDateTime.parse(end_date))
                .addValue("bonus",bonus_points);
            String insertEvent = "INSERT INTO SpecialEvent (animal_id, StartDate, EndDate, BonusPoints) VALUES (:animal_id,:now,:end,:bonus);";
            jdbcTemplate.update(insertEvent,source3);

            return new ModelAndView("redirect:http://localhost:7777/"+user_id+"/events?msg=OK");
        }
        return new ModelAndView("redirect:http://localhost:7777/"+user_id+"/events?msg=KO");   
    }

    @RequestMapping("getSpecialEvents")
    public ResponseEntity<List<Map<String, Object>>> getSpecialEvents(){
        LocalDateTime start_date=LocalDateTime.now();
        MapSqlParameterSource source1 = new MapSqlParameterSource()
            .addValue("now", start_date);
        String getSpecialEventsQuery="select a.a_name as animalName, s.enddate as endDate, round(100*s.bonuspoints/a.std_points,2) as bonusPoints from specialevent s join animal a on s.animal_id=a.a_id where :now<enddate";
        List<Map<String, Object>> specialEvents = jdbcTemplate.queryForList(getSpecialEventsQuery, source1);
        return ResponseEntity.ok(specialEvents);
    }
}
