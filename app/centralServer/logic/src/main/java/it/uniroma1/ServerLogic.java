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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.List;

import java.io.IOException;

import javax.sql.DataSource;
import javax.validation.constraints.Null;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

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

            Integer exampleUserId=new Integer(999);
        
            if (jsonNode.has("animalName")) {
                String animalName = jsonNode.get("animalName").asText();
                if (!animalName.isEmpty()) {
                    return certificateManagement(animalName,exampleUserId);
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

    @RequestMapping(value = "/userSignUp", method = RequestMethod.POST)
    public ModelAndView userSignUp(@RequestParam(value = "email", required = true) String email, 
                                @RequestParam(value = "password", required = true) String password,
                                @RequestParam(value = "username", required = true) String username,
                                @RequestParam(value = "name", required = false) String name,
                                @RequestParam(value = "surname", required = false) String surname,
                                @RequestParam(value = "dateofbirth", required = false) String dateofbirth,
                                @RequestParam(value = "confirmpassword", required = true) String confirmPassword) {
        if(password.equals(confirmPassword)){
            // compute the hash of the password
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            password = encoder.encode(password);

            // transform dateofbirth in the right format
            LocalDateTime dob = null;
            if(!dateofbirth.isEmpty()){
                dateofbirth+="T00:00:00";
                dob = LocalDateTime.parse(dateofbirth);
            }

            MapSqlParameterSource source1 = new MapSqlParameterSource()
                    .addValue("u_email", email);
            String checkEmail = "SELECT count(*) from users where email = :u_email";
            Integer count = jdbcTemplate.queryForObject(checkEmail, source1, Integer.class);
            if (count != null && count > 0) {
                // Email already used
                Integer err=90;
                return new ModelAndView("redirect:http://localhost:3000/LoginUser.html?err="+err);
            } else {
                // Save user information in the database
                MapSqlParameterSource source2 = new MapSqlParameterSource()
                    .addValue("u_email", email)
                    .addValue("hpassw", password)
                    .addValue("username", username)
                    .addValue("firstname", name)
                    .addValue("surname", surname)
                    .addValue("birthday", dob);
                String insertQuery = "INSERT INTO users (email, passw, username, firstname, surname, birthday) VALUES (:u_email, :hpassw, :username, :firstname, :surname, :birthday)";
                jdbcTemplate.update(insertQuery, source2);
                // Redirect to login page
                Integer suc=99;
                return new ModelAndView("redirect:http://localhost:3000/LoginUser.html?suc="+suc);
            }
        }
        else {
            // Error: password is not equal to confirmPassword
            Integer err2=91;
            return new ModelAndView("redirect:http://localhost:3000/RegistrationUser.html?err2="+err2);
        }
    }
    
    @RequestMapping(value = "/userSignIn", method = RequestMethod.POST)
    public ModelAndView userSignIn(@RequestParam(value = "email", required = true) String email, @RequestParam(value = "password", required = true) String password) {
        MapSqlParameterSource source1 = new MapSqlParameterSource().addValue("u_email", email);
        String checkEmail = "SELECT count(*) from users where email = :u_email";
        Integer count = jdbcTemplate.queryForObject(checkEmail, source1, Integer.class);
        if (count == null || count <= 0) {
            // User not in db
            Integer err2=95;
            return new ModelAndView("redirect:http://localhost:3000/RegistrationUser.html?err2="+err2);
        }
        
        String insertQuery = "select user_id, passw from users where email = :u_email"; // Seleziona anche l'id dell'utente
        // Recupera l'id e la password dell'utente dal database
        Map<String, Object> userMap = jdbcTemplate.queryForMap(insertQuery, source1);

        String realPassword = (String) userMap.get("passw");
        int userId = (int) userMap.get("user_id"); // Ottieni l'id dell'utente
        //System.out.println(userId);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (encoder.matches(password, realPassword)) {
            // Se il login è corretto, reindirizza alla pagina personal page con l'id utente allegato
            ModelAndView modelAndView = new ModelAndView("redirect:http://localhost:3000/PersonalPage.html?userId=" + userId);
            //modelAndView.addObject("userId", userId); // Aggiungi l'id utente come attributo per il reindirizzamento
            return modelAndView;
        } else {
            // Se le credenziali di accesso sono incorrette, reindirizza alla pagina di login con un messaggio di errore
            Integer err=990;
            ModelAndView modelAndView = new ModelAndView("redirect:http://localhost:3000/LoginUser.html?err="+err);
            return modelAndView;
        }
    }

    @RequestMapping(value = "/operatorSignUp", method = RequestMethod.POST)
    public ModelAndView operatorSignUp(@RequestParam(value = "o_email", required = true) String o_email, 
                                @RequestParam(value = "passw", required = true) String passw,
                                @RequestParam(value = "firstname", required = false) String firstname,
                                @RequestParam(value = "surname", required = false) String surname,
                                @RequestParam(value = "dateofbirth", required = false) String dateofbirth,
                                @RequestParam(value = "confirmpassword", required = true) String confirmPassword) {
        if(passw.equals(confirmPassword)){
            // compute the hash of the password
            BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
            passw = encoder.encode(passw);

            // transform dateofbirth in the right format
            LocalDateTime dob = null;
            if(!dateofbirth.isEmpty()){
                dateofbirth+="T00:00:00";
                dob = LocalDateTime.parse(dateofbirth);
            }

            MapSqlParameterSource source1 = new MapSqlParameterSource().addValue("o_email", o_email);
            String checkEmail = "SELECT count(*) from operator where o_email = :o_email";
            Integer count = jdbcTemplate.queryForObject(checkEmail, source1, Integer.class);
            if (count != null && count > 0) {
                // Email already used
                Integer err=90;
                return new ModelAndView("redirect:http://localhost:3000/LoginOperator.html?err="+err);
            } else {
                //genera un code randomico mai uguale ai precedenti
                UUID uuid = UUID.randomUUID();
                String code = uuid.toString();
                // Save user information in the database
                MapSqlParameterSource source2 = new MapSqlParameterSource()
                    .addValue("o_email", o_email)
                    .addValue("code", code)
                    .addValue("passw", passw)
                    .addValue("firstname", firstname)
                    .addValue("surname", surname)
                    .addValue("birthday", dob);
                String insertQuery = "INSERT INTO operator (o_email, code, passw, firstname, surname, birthday) VALUES (:o_email, :code, :passw, :firstname, :surname, :birthday)";
                jdbcTemplate.update(insertQuery, source2);
                // Redirect to login page
                Integer suc=99;
                return new ModelAndView("redirect:http://localhost:3000/LoginOperator.html?suc="+suc);
            }
        }
        else {
            // Error: password is not equal to confirmPassword
            Integer err2=91;
            return new ModelAndView("redirect:http://localhost:3000/RegistrationOperator.html?err2="+err2);
        }
    }
    
    @RequestMapping(value = "/operatorSignIn", method = RequestMethod.POST)
    public ModelAndView operatorSignIn(@RequestParam(value = "o_email", required = true) String o_email, @RequestParam(value = "passw", required = true) String passw) {
        MapSqlParameterSource source1 = new MapSqlParameterSource().addValue("o_email", o_email);
        String checkEmail = "SELECT count(*) from operator where o_email = :o_email";
        Integer count = jdbcTemplate.queryForObject(checkEmail, source1, Integer.class);
        if (count == null || count <= 0) {
            // Operator not in db
            Integer err2=95;
            return new ModelAndView("redirect:http://localhost:3000/RegistrationOperator.html?err2="+err2);
        }
        
        String insertQuery = "select code, passw from operator where o_email = :o_email"; // Seleziona anche l'id dell'utente
        // Recupera l'id e la password dell'utente dal database
        Map<String, Object> opMap = jdbcTemplate.queryForMap(insertQuery, source1);

        String realPassword = (String) opMap.get("passw");
        String opCode = (String) opMap.get("code"); // Ottieni l'id dell'utente
        //System.out.println(userId);

        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (encoder.matches(passw, realPassword)) {
            // Se il login è corretto, reindirizza alla pagina personal page con l'id utente allegato
            ModelAndView modelAndView = new ModelAndView("redirect:http://localhost:3000/PersonalPageOperator.html?userId=" + opCode);
            //modelAndView.addObject("userId", userId); // Aggiungi l'id utente come attributo per il reindirizzamento
            return modelAndView;
        } else {
            // Se le credenziali di accesso sono incorrette, reindirizza alla pagina di login con un messaggio di errore
            Integer err=990;
            ModelAndView modelAndView = new ModelAndView("redirect:http://localhost:3000/LoginOperator.html?err="+err);
            return modelAndView;
        }
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
}
