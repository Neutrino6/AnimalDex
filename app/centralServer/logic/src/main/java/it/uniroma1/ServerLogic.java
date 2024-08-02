package it.uniroma1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
import java.io.IOException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;



@RestController
public class ServerLogic {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @Autowired
    private HttpServletResponse response;

    @RequestMapping("/hello")
    public String hello(){
        return "Hello this server is running";
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

            String cookieValue = DigestUtils.sha256Hex("LOGIN:" + userId);
            Cookie authCookie = new Cookie("authCookie", cookieValue);
            authCookie.setPath("/");
            response.addCookie(authCookie);

            ModelAndView modelAndView = new ModelAndView("redirect:http://localhost:3000/Redirect/" + userId);
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
            ModelAndView modelAndView = new ModelAndView("redirect:http://localhost:3000/PersonalPageOperator/" + opCode);
            //modelAndView.addObject("userId", userId); // Aggiungi l'id utente come attributo per il reindirizzamento
            return modelAndView;
        } else {
            // Se le credenziali di accesso sono incorrette, reindirizza alla pagina di login con un messaggio di errore
            Integer err=990;
            ModelAndView modelAndView = new ModelAndView("redirect:http://localhost:3000/LoginOperator.html?err="+err);
            return modelAndView;
        }
    }


    @RequestMapping(value = "/PersonalPageUser")
    public ResponseEntity<String> PersonalePageUser(@RequestParam(value = "user_id") Integer user_id) throws JsonProcessingException {
        MapSqlParameterSource source1 = new MapSqlParameterSource().addValue("user_id", user_id);
        String checkId = "SELECT count(*) from users where user_id = :user_id";
        Integer count = jdbcTemplate.queryForObject(checkId, source1, Integer.class);
        if (count == null || count <= 0) {
            // User not in db
            Integer err2=95;
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("user not found");
        }
        
        String insertQuery = "select * from users where user_id = :user_id"; // Seleziona anche l'id dell'utente
        // Recupera l'id e la password dell'utente dal database
        Map<String, Object> userMap = jdbcTemplate.queryForMap(insertQuery, source1);

        
        //String passw = (String) userMap.get("passw");
        String username = (String) userMap.get("username"); 
        String email = (String) userMap.get("email");
        String firstname = (String) userMap.get("firstname"); 
        String surname = (String) userMap.get("surname");
        Date dob = (Date) userMap.get("birthday");
        System.out.println(dob);
        String strdob=null;
        if(dob!=null){
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            strdob = dateFormat.format(dob);
        }
        System.out.println(strdob);
        Integer points = (Integer) userMap.get("points");
        Integer fav_animal = (Integer) userMap.get("fav_animal"); 
        Boolean forum_not = (Boolean) userMap.get("forum_notify");
        Boolean emergency_not = (Boolean) userMap.get("emergency_notify");
        Boolean admin = (Boolean) userMap.get("administrator");
        byte[] profileImage = (byte[]) userMap.get("profile_image");
        
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode responseJson = mapper.createObjectNode();

        //responseJson.put("passw", passw);
        responseJson.put("username", username);
        responseJson.put("email", email);
        responseJson.put("firstname", firstname);
        responseJson.put("surname", surname);
        responseJson.put("points", points);
        responseJson.put("dob", strdob);
        responseJson.put("fav_animal", fav_animal);
        responseJson.put("emergency_not", emergency_not);
        responseJson.put("forum_not", forum_not);
        responseJson.put("profile_image", profileImage);
        responseJson.put("admin", admin);

        String jsonResponse = mapper.writeValueAsString(responseJson);
        return ResponseEntity.ok(jsonResponse);
    }

    @RequestMapping(value = "/PersonalPageOperator")
    public ResponseEntity<String> PersonalePageOperator(@RequestParam(value = "code") String code) throws JsonProcessingException {
        MapSqlParameterSource source1 = new MapSqlParameterSource().addValue("code", code);
        String checkId = "SELECT count(*) from operator where code = :code";
        Integer count = jdbcTemplate.queryForObject(checkId, source1, Integer.class);
        if (count == null || count <= 0) {
            // Operator not in db
            Integer err2=95;
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("operator not found");
        }
        
        String insertQuery = "select * from operator where code = :code"; // Seleziona anche l'id dell'utente
        // Recupera l'id e la password dell'utente dal database
        Map<String, Object> operMap = jdbcTemplate.queryForMap(insertQuery, source1);

        
        //String passw = (String) Map.get("passw");
        String codeOp = (String) operMap.get("code"); 
        String email = (String) operMap.get("o_email");
        String firstname = (String) operMap.get("firstname"); 
        String surname = (String) operMap.get("surname");
        Date dob = (Date) operMap.get("birthday");
        System.out.println(dob);
        String strdob=null;
        if(dob!=null){
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            strdob = dateFormat.format(dob);
        }
        System.out.println(strdob);
        
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode responseJson = mapper.createObjectNode();

        //responseJson.put("passw", passw);
        responseJson.put("codeOp", codeOp);
        responseJson.put("email", email);
        responseJson.put("firstname", firstname);
        responseJson.put("surname", surname);
        responseJson.put("dob", strdob);

        String jsonResponse = mapper.writeValueAsString(responseJson);
        return ResponseEntity.ok(jsonResponse);
    }

    @RequestMapping(value = "/deleteAccountUser", method = RequestMethod.POST)
    public ModelAndView deleteAccountUser(@RequestParam(value = "user_id") Integer userId) {
        safeUserDeletion(userId);
        return new ModelAndView("redirect:http://localhost:3000/RegistrationUser.html");
    } 

    @Transactional
    private void safeUserDeletion(Integer userId){
        MapSqlParameterSource source = new MapSqlParameterSource().addValue("user_id", userId);
        String deleteCertifiacates = "DELETE FROM certification WHERE user_id = :user_id";
        jdbcTemplate.update(deleteCertifiacates, source);
        String deleteQuery = "DELETE FROM users WHERE user_id = :user_id";
        jdbcTemplate.update(deleteQuery, source);
    }

    @RequestMapping(value = "/deleteAccountOperator", method = RequestMethod.POST)
    public ModelAndView deleteAccountOperator(@RequestParam(value = "operCode") String operCode) {
        safeOperDeletion(operCode);
        return new ModelAndView("redirect:http://localhost:3000/RegistrationOperator.html");
    } 

    @Transactional
    private void safeOperDeletion(String operCode){
        MapSqlParameterSource source = new MapSqlParameterSource().addValue("operCode", operCode);
        //String deleteCertifiacates = "DELETE FROM certification WHERE user_id = :user_id";
        //jdbcTemplate.update(deleteCertifiacates, source);
        String deleteQuery = "DELETE FROM operator WHERE code = :operCode";
        jdbcTemplate.update(deleteQuery, source);
    }

    @RequestMapping(value = "/changeCredentialsUser", method = RequestMethod.POST)
    public ModelAndView changeCredentialsUser(
    @RequestParam(value = "user_id") Integer userId, 
    @RequestParam(value = "email") String email, 
    @RequestParam(value = "username") String username, 
    @RequestParam(value = "name") String firstname, 
    @RequestParam(value = "surname") String surname, 
    @RequestParam(value = "password") String password, 
    @RequestParam(value = "dateofbirth") String dob, 
    @RequestParam(value = "animal") Integer animal, 
    @RequestParam(value = "forum") Boolean forum, 
    @RequestParam(value = "emergencies") Boolean emergencies) {
        System.out.println(forum);
        LocalDateTime dob2 = null;
        if(!dob.isEmpty()){
            dob+="T00:00:00";
            dob2 = LocalDateTime.parse(dob);
        }
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        password = encoder.encode(password);
        MapSqlParameterSource source = new MapSqlParameterSource()
        .addValue("user_id", userId)
        .addValue("email", email)
        .addValue("username", username)
        .addValue("firstname", firstname)
        .addValue("surname", surname)
        .addValue("password", password)
        .addValue("dob", dob2)
        .addValue("animal", animal)
        .addValue("forum", forum)
        .addValue("emergencies", emergencies);
        String updateQuery = "UPDATE users SET " +
                         "email = :email, " +
                         "username = :username, " +
                         "firstname = :firstname, " +
                         "surname = :surname, " +
                         "passw = :password, " +
                         "birthday = :dob, " +
                         "fav_animal = :animal, " +
                         "forum_notify = :forum, " +
                         "emergency_notify = :emergencies " +
                         "WHERE user_id = :user_id";
        jdbcTemplate.update(updateQuery, source);
        return new ModelAndView("redirect:http://localhost:3000/Redirect/" + userId);
    }

    @RequestMapping(value = "/changeProfileImage", method = RequestMethod.POST)
        public ModelAndView handleFileUpload(@RequestParam(value = "user_id") Integer userId , @RequestParam(value = "profile_image") MultipartFile file) throws IOException {
 
            byte[] imageBytes = file.getBytes();
            MapSqlParameterSource source = new MapSqlParameterSource()
            .addValue("user_id", userId)
            .addValue("profile_image", imageBytes);
            
            String updateQuery = "UPDATE users SET " +
                            "profile_image = :profile_image " +
                            "WHERE user_id = :user_id";
            jdbcTemplate.update(updateQuery, source);

            return new ModelAndView("redirect:http://localhost:3000/Redirect/" + userId);
        }

    @RequestMapping(value = "/UsersList", method = RequestMethod.POST)
        public String usersList(@RequestParam(value = "user_id") Integer userId, @RequestParam(value = "admin") Boolean admin ) {

            MapSqlParameterSource source = new MapSqlParameterSource()
                .addValue("user_id", userId)
                .addValue("admin", admin);

            // Verifica se l'utente esiste
            String verifyAdminQuery = "SELECT COUNT(*) FROM users WHERE user_id = :user_id";
            Integer adminCount = jdbcTemplate.queryForObject(verifyAdminQuery, source, Integer.class);

            if (adminCount == null || adminCount <= 0) {
                // User not in db
                return "<html><body><h1>Error</h1><p>User not found in database</p></body></html>";
            }

            if (!admin) {
                // User not admin
                return "<html><body><h1>Error</h1><p>User is not an admin</p></body></html>";
            }

            // Recupera tutti gli utenti dal database, escludendo la password
            String getUsersQuery = "SELECT user_id, email, username, firstname, surname, points, birthday, fav_animal, forum_notify, emergency_notify, administrator FROM users";
            List<Map<String, Object>> users = jdbcTemplate.queryForList(getUsersQuery, new MapSqlParameterSource());

            StringBuilder html = new StringBuilder();
            html.append("<html>");
            html.append("<head><title>Users List</title></head>");
            html.append("<body>");
            html.append("<h1>Users List</h1>");
            html.append("<table border='1'>");
            html.append("<tr>")
                .append("<th>User ID</th>")
                .append("<th>Email</th>")
                .append("<th>Username</th>")
                .append("<th>First Name</th>")
                .append("<th>Surname</th>")
                .append("<th>Points</th>")
                .append("<th>Birthday</th>")
                .append("<th>Fav Animal</th>")
                .append("<th>Forum Notify</th>")
                .append("<th>Emergency Notify</th>")
                .append("<th>Administrator</th>")
                .append("</tr>");

            for (Map<String, Object> user : users) {
                html.append("<tr>")
                    .append("<td>").append(user.get("user_id")).append("</td>")
                    .append("<td>").append(user.get("email")).append("</td>")
                    .append("<td>").append(user.get("username")).append("</td>")
                    .append("<td>").append(user.get("firstname")).append("</td>")
                    .append("<td>").append(user.get("surname")).append("</td>")
                    .append("<td>").append(user.get("points")).append("</td>")
                    .append("<td>").append(user.get("birthday")).append("</td>")
                    .append("<td>").append(user.get("fav_animal")).append("</td>")
                    .append("<td>").append(user.get("forum_notify")).append("</td>")
                    .append("<td>").append(user.get("emergency_notify")).append("</td>")
                    .append("<td>").append(user.get("administrator")).append("</td>")
                    .append("</tr>");
            }

            html.append("</table>");
            html.append("</body>");
            html.append("</html>");

            // Ritorna l'HTML generato
            return html.toString();
        }

    @RequestMapping(value = "/UserOauth", method = RequestMethod.POST)
    public ResponseEntity<String> UserOauth(@RequestBody String requestBody){
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(requestBody);

            if (jsonNode.has("email") && jsonNode.has("name")) {
                String email = jsonNode.get("email").asText();
                String username = jsonNode.get("name").asText();
                if (!email.isEmpty() && !username.isEmpty()) {
                    return oauthManagement(email,username);
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error");
                }
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error");
        }
    }

    @Transactional
    private ResponseEntity<String> oauthManagement(String email, String username) {
        
        MapSqlParameterSource source1 = new MapSqlParameterSource()
            .addValue("email",email);
        String checkExistenceforSameEmail = "SELECT count(*) FROM users WHERE :email=email and passw is not null";
        String checkExistence = "SELECT count(*) FROM users WHERE :email=email";
        Integer count0 = jdbcTemplate.queryForObject(checkExistenceforSameEmail, source1, Integer.class);
        Integer count1 = jdbcTemplate.queryForObject(checkExistence, source1, Integer.class);
        if(count0!=null && count0==0 && count1!=null){
            //first time, create a new user
            if(count1==0){
                MapSqlParameterSource source2 = new MapSqlParameterSource()
                    .addValue("username", username)
                    .addValue("email",email);
                String insertUser= "INSERT INTO users (email, username) VALUES (:email, :username)";
                jdbcTemplate.update(insertUser, source2);
            }
            //user already registered
            String getUserId = "select user_id from users where email = :email";
            Integer userId = jdbcTemplate.queryForObject(getUserId, source1, Integer.class);
            return ResponseEntity.ok(userId.toString());
        }
        else{
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error");
        }
    }

}
