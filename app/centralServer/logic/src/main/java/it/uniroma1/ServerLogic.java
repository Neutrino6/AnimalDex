package it.uniroma1;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
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
//import javax.validation.constraints.Null;
import javax.validation.constraints.Null;

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
        Boolean admin = (Boolean) userMap.get("administrator");
        byte[] profileImage = (byte[]) userMap.get("profile_image");
        
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode responseJson = mapper.createObjectNode();

        //Retrieve all animals and add them to the JSON response**
        String animalQuery = "SELECT a.a_name FROM animal a ";
        List<Map<String, Object>> animalList = jdbcTemplate.queryForList(animalQuery, source1);

        // Iterating over the list of animals and adding each to the JSON response
        for (int i = 0; i < animalList.size(); i++) {
            String animalName = (String) animalList.get(i).get("a_name");
            responseJson.put("animal" + (i+1), animalName);
        }
        String fav_animal_name;
        if(fav_animal!=null) {
            MapSqlParameterSource source2 = new MapSqlParameterSource().addValue("fav_animal", fav_animal);
            String fav_animalQuery = "select a_name from animal where a_id = :fav_animal"; // Seleziona anche l'id dell'utente
            // Recupera l'id e la password dell'utente dal database
            Map<String, Object> fav_animalMap = jdbcTemplate.queryForMap(fav_animalQuery, source2);
            fav_animal_name = (String) fav_animalMap.get("a_name");
        }
        else {
            fav_animal_name = "No Favourite Animal Chosen";
        }

        responseJson.put("animalListSize", animalList.size());
        //responseJson.put("passw", passw);
        responseJson.put("username", username);
        responseJson.put("email", email);
        responseJson.put("firstname", firstname);
        responseJson.put("surname", surname);
        responseJson.put("points", points);
        responseJson.put("dob", strdob);
        responseJson.put("fav_animal", fav_animal);
        responseJson.put("fav_animal_name", fav_animal_name);
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
    @RequestParam(value = "dateofbirth") String dob, 
    @RequestParam(value = "animal") Integer animal) {
        LocalDateTime dob2 = null;
        if(!dob.isEmpty()){
            dob+="T00:00:00";
            dob2 = LocalDateTime.parse(dob);
        }
        MapSqlParameterSource source = new MapSqlParameterSource()
        .addValue("user_id", userId)
        .addValue("email", email)
        .addValue("username", username)
        .addValue("firstname", firstname)
        .addValue("surname", surname)
        .addValue("dob", dob2)
        .addValue("animal", animal);
        String updateQuery = "UPDATE users SET " +
                         "email = :email, " +
                         "username = :username, " +
                         "firstname = :firstname, " +
                         "surname = :surname, " +
                         "birthday = :dob, " +
                         "fav_animal = :animal " +
                         "WHERE user_id = :user_id";
        jdbcTemplate.update(updateQuery, source);
        return new ModelAndView("redirect:http://localhost:3000/Redirect/" + userId);
    }

    @RequestMapping(value = "/changePasswordUser", method = RequestMethod.POST)
    public ModelAndView changeCredentialsUser(
    @RequestParam(value = "user_id") Integer userId,
    @RequestParam(value = "password") String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        password = encoder.encode(password);
        MapSqlParameterSource source = new MapSqlParameterSource()
        .addValue("user_id", userId)
        .addValue("password", password);
        String updateQuery = "UPDATE users SET " +
                         "passw = :password " +
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

    @RequestMapping(value = "/banUser", method = RequestMethod.POST)
        public ModelAndView banUser(@RequestParam(value = "user_id") Integer userId2, @RequestParam(value = "user_id_admin") Integer userId) {
            safeUserBan(userId2);
            return new ModelAndView("redirect:http://localhost:3000/Redirect/" + userId);
        } 
    
    @Transactional
        private void safeUserBan(Integer userId2){
            MapSqlParameterSource source = new MapSqlParameterSource().addValue("user_id", userId2);
            String deleteCertifiacates = "DELETE FROM certification WHERE user_id = :user_id";
            jdbcTemplate.update(deleteCertifiacates, source);
            String deleteQuery = "DELETE FROM users WHERE user_id = :user_id";
            jdbcTemplate.update(deleteQuery, source);
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
                    .append("<td>")
                    .append("<form action='/banUser' method='post'>")
                    .append("<input type='hidden' name='user_id' value='").append(user.get("user_id")).append("'>")
                    .append("<input type='hidden' name='user_id_admin' value='").append(userId).append("'>")
                    .append("<button type='submit'> Permanent Ban </button>")
                    .append("</form>")
                    .append("</td>")
                    .append("</tr>");
            }

            html.append("</table>");
            html.append("</body>");
            html.append("</html>");

            // Ritorna l'HTML generato
            return html.toString();
        }

    @RequestMapping(value = "/banOperator", method = RequestMethod.POST)
        public ModelAndView banOperator(@RequestParam(value = "operCode") String operCode, @RequestParam(value = "user_id_admin") String userId) {
            safeOperBan(operCode);
            return new ModelAndView("redirect:http://localhost:3000/Redirect/" + userId);
        } 

    @Transactional
        private void safeOperBan(String operCode){
            MapSqlParameterSource source = new MapSqlParameterSource().addValue("operCode", operCode);
            //String deleteCertifiacates = "DELETE FROM certification WHERE user_id = :user_id";
            //jdbcTemplate.update(deleteCertifiacates, source);
            String deleteQuery = "DELETE FROM operator WHERE code = :operCode";
            jdbcTemplate.update(deleteQuery, source);
        }

    @RequestMapping(value = "/OpersList", method = RequestMethod.POST)
        public String opersList(@RequestParam(value = "user_id") Integer userId, @RequestParam(value = "admin") Boolean admin) {

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
            String getUsersQuery = "SELECT o_email, code, firstname, surname, birthday FROM operator";
            List<Map<String, Object>> opers = jdbcTemplate.queryForList(getUsersQuery, new MapSqlParameterSource());

            StringBuilder html = new StringBuilder();
            html.append("<html>");
            html.append("<head><title>Operators List</title></head>");
            html.append("<body>");
            html.append("<h1>Operators List</h1>");
            html.append("<table border='1'>");
            html.append("<tr>")
                .append("<th>Oper Code</th>")
                .append("<th>Email</th>")
                .append("<th>First Name</th>")
                .append("<th>Surname</th>")
                .append("<th>Birthday</th>")
                .append("</tr>");

            for (Map<String, Object> oper : opers) {
                html.append("<tr>")
                    .append("<td>").append(oper.get("code")).append("</td>")
                    .append("<td>").append(oper.get("o_email")).append("</td>")
                    .append("<td>").append(oper.get("firstname")).append("</td>")
                    .append("<td>").append(oper.get("surname")).append("</td>")
                    .append("<td>").append(oper.get("birthday")).append("</td>")
                    .append("<td>")
                    .append("<form action='/banOperator' method='post'>")
                    .append("<input type='hidden' name='operCode' value='").append(oper.get("code")).append("'>")
                    .append("<input type='hidden' name='user_id_admin' value='").append(userId).append("'>")
                    .append("<button type='submit'> Permanent Ban </button>")
                    .append("</form>")
                    .append("</td>")
                    .append("</tr>");
            }

            html.append("</table>");
            html.append("</body>");
            html.append("</html>");

            // Ritorna l'HTML generato
            return html.toString();
        }

        //FORUM-----------------------------------------------------------------

        @RequestMapping(value = "/addComment", method = RequestMethod.POST)
        public ModelAndView addComment(@RequestParam(value = "user_id") Integer userId, @RequestParam(value = "admin") Boolean admin, @RequestParam(value = "c_content") String c_content, 
                                @RequestParam(value = "username") String username, @RequestParam(value = "date") String date, @RequestParam(value = "sort") String sort) {

            LocalDateTime date2 = null;
            if(!date.isEmpty()){
                date+="T00:00:00";
                date2 = LocalDateTime.parse(date);
            }
            //Esegui l'aggiunta del commento al db e reindirizza a forum una volta finito

            // Creazione di un'istanza MapSqlParameterSource per i parametri di query
            MapSqlParameterSource source = new MapSqlParameterSource()
            .addValue("user_id", userId)
            .addValue("username", username)
            .addValue("c_date", date2) // Aggiungi la data corrente
            .addValue("c_content", c_content);
            
            // Query SQL per inserire il commento nel database
            String insertCommentQuery = "INSERT INTO comment (user_id, username, c_date, c_content) VALUES (:user_id, :username, :c_date,  :c_content)";

            // Esegui l'inserimento del commento
            jdbcTemplate.update(insertCommentQuery, source);

            // Costruisci una risposta HTML con uno script di redirezione POST
            //System.out.println(admin);

            return new ModelAndView("redirect:http://localhost:3000/Forum/"+userId+"/"+admin+"/"+sort);
        }

        @RequestMapping(value = "/addReply", method = RequestMethod.POST)
        public ModelAndView addReply(@RequestParam(value = "user_id") Integer userId, @RequestParam(value = "admin") Boolean admin, @RequestParam(value = "c_content") String c_content, 
                                       @RequestParam(value = "username") String username, @RequestParam(value = "date") String date, @RequestParam(value = "c_id") Integer c_id, @RequestParam(value = "sort") String sort) {

            LocalDateTime date2 = null;
            if(!date.isEmpty()){
                date+="T00:00:00";
                date2 = LocalDateTime.parse(date);
            }
            //Esegui l'aggiunta del commento al db e reindirizza a forum una volta finito

            // Creazione di un'istanza MapSqlParameterSource per i parametri di query
            MapSqlParameterSource source = new MapSqlParameterSource()
            .addValue("user_id", userId)
            .addValue("username", username)
            .addValue("c_date", date2) // Aggiungi la data corrente
            .addValue("c_content", c_content)
            .addValue("c_id", c_id);
            
            

            // Query SQL per inserire il commento nel database
            String insertCommentQuery = "INSERT INTO reply (user_id, username, c_date, c_content, c_id_orig) VALUES (:user_id, :username, :c_date, :c_content, :c_id)";

            // Esegui l'inserimento del commento
            jdbcTemplate.update(insertCommentQuery, source);

            // Costruisci una risposta HTML con uno script di redirezione POST
            System.out.println(admin);
            
            return new ModelAndView("redirect:http://localhost:3000/Forum/"+userId+"/"+admin+"/"+sort);
        }

        @RequestMapping(value = "/deleteComment", method = RequestMethod.POST)
        public ModelAndView deleteComment(@RequestParam(value = "user_id") Integer userId, @RequestParam(value = "c_id") Integer c_id, @RequestParam(value = "admin") Boolean admin, @RequestParam(value = "sort") String sort) {
            safeDeleteComment(c_id); 

            // Costruisci una risposta HTML con uno script di redirezione POST
            
            return new ModelAndView("redirect:http://localhost:3000/Forum/"+userId+"/"+admin+"/"+sort);
        }

         @Transactional
        private void safeDeleteComment(Integer c_id){
             MapSqlParameterSource source = new MapSqlParameterSource()
            .addValue("c_id", c_id);
           
            // Query SQL per inserire il commento nel database
            String deleteCommentQuery = "DELETE FROM comment WHERE c_id = :c_id";

            // Esegui l'inserimento del commento
            jdbcTemplate.update(deleteCommentQuery, source);
            
        }

        @RequestMapping(value = "/deleteReply", method = RequestMethod.POST)
        public ModelAndView deleteReply(@RequestParam(value = "user_id") Integer userId, @RequestParam(value = "c_id") Integer c_id, @RequestParam(value = "admin") Boolean admin, @RequestParam(value = "sort") String sort) {
            safeDeleteReply(c_id); 

            // Costruisci una risposta HTML con uno script di redirezione POST
            
            return new ModelAndView("redirect:http://localhost:3000/Forum/"+userId+"/"+admin+"/"+sort);
        }

        @Transactional
        private void safeDeleteReply(Integer c_id){
             MapSqlParameterSource source = new MapSqlParameterSource()
            .addValue("c_id", c_id);
           
            // Query SQL per inserire il commento nel database
            String deleteCommentQuery = "DELETE FROM reply WHERE c_id_reply = :c_id";

            // Esegui l'inserimento del commento
            jdbcTemplate.update(deleteCommentQuery, source);
            
        }

        @RequestMapping(value = "/modifyComment", method = RequestMethod.POST)
        public ModelAndView modifyComment(@RequestParam(value = "user_id") Integer userId, @RequestParam(value = "admin") Boolean admin, @RequestParam(value = "c_content") String c_content, 
                                @RequestParam(value = "c_id") Integer c_id, @RequestParam(value = "modify_date") String date, @RequestParam(value = "sort") String sort) {

            LocalDateTime date2 = null;
            if(!date.isEmpty()){
                date+="T00:00:00";
                date2 = LocalDateTime.parse(date);
            }
            //Esegui l'aggiunta del commento al db e reindirizza a forum una volta finito

            // Creazione di un'istanza MapSqlParameterSource per i parametri di query
            MapSqlParameterSource source = new MapSqlParameterSource()
            .addValue("user_id", userId)
            .addValue("c_id", c_id)
            .addValue("modify_date", date2) // Aggiungi la data corrente
            .addValue("c_content", c_content);
            
            // Query SQL per inserire il commento nel database
            String updateCommentQuery = "UPDATE comment SET modify_date = :modify_date, c_content = :c_content WHERE user_id=:user_id AND c_id = :c_id";

            // Esegui l'inserimento del commento
            jdbcTemplate.update(updateCommentQuery, source);

            // Costruisci una risposta HTML con uno script di redirezione POST
            //System.out.println(admin);
            return new ModelAndView("redirect:http://localhost:3000/Forum/"+userId+"/"+admin+"/"+sort);
        }

        @RequestMapping(value = "/modifyReply", method = RequestMethod.POST)
        public ModelAndView modifyReply(@RequestParam(value = "user_id") Integer userId, @RequestParam(value = "admin") Boolean admin, @RequestParam(value = "c_content") String c_content, 
                                @RequestParam(value = "c_id") Integer c_id, @RequestParam(value = "modify_date") String date, @RequestParam(value = "sort") String sort) {

            LocalDateTime date2 = null;
            if(!date.isEmpty()){
                date+="T00:00:00";
                date2 = LocalDateTime.parse(date);
            }
            //Esegui l'aggiunta del commento al db e reindirizza a forum una volta finito

            // Creazione di un'istanza MapSqlParameterSource per i parametri di query
            MapSqlParameterSource source = new MapSqlParameterSource()
            .addValue("user_id", userId)
            .addValue("c_id", c_id)
            .addValue("modify_date", date2) // Aggiungi la data corrente
            .addValue("c_content", c_content);
            
            // Query SQL per inserire il commento nel database
            String updateReplyQuery = "UPDATE reply SET modify_date = :modify_date, c_content = :c_content WHERE user_id=:user_id AND c_id_reply = :c_id";

            // Esegui l'inserimento del commento
            jdbcTemplate.update(updateReplyQuery, source);

            // Costruisci una risposta HTML con uno script di redirezione POST
            //System.out.println(admin);
            return new ModelAndView("redirect:http://localhost:3000/Forum/"+userId+"/"+admin+"/"+sort);
        }
        
        @RequestMapping(value = "/Forum")
        public ResponseEntity<String> Forum(@RequestParam(value = "user_id") Integer userId, @RequestParam(value = "admin") Boolean admin, @RequestParam(value = "sort") String sort) throws JsonProcessingException {

                MapSqlParameterSource source = new MapSqlParameterSource()
                    .addValue("user_id", userId);

                // Verifica se l'utente esiste
                String verifyuserQuery = "SELECT COUNT(*) FROM users WHERE user_id = :user_id";
                Integer Count = jdbcTemplate.queryForObject(verifyuserQuery, source, Integer.class);

                if (Count == null || Count <= 0) {
                    // User not in db
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("user not found");
                }

                // Recupera tutti i commenti e le risposte dal database, escludendo la password
              /*   String getCommentsQuery = "SELECT c_id, user_id, username, c_date, c_content, modify_date FROM comment"; */
                 String getCommentsQuery = "SELECT c_id, user_id, username, c_date, c_content, modify_date, " +
                                            "(SELECT COUNT(*) FROM reply WHERE c_id_orig = comment.c_id) AS reply_count " +
                                            "FROM comment";
                String getRepliesQuery = "SELECT c_id_reply, user_id, username, c_date, c_content, c_id_orig, modify_date FROM reply";
                List<Map<String, Object>> comments = jdbcTemplate.queryForList(getCommentsQuery, new MapSqlParameterSource());
                List<Map<String, Object>> replies = jdbcTemplate.queryForList(getRepliesQuery, new MapSqlParameterSource());
                String userQuery = "SELECT username FROM users WHERE user_id = :user_id";
                String username = jdbcTemplate.queryForObject(userQuery, source, String.class);

/* 
                LocalDateTime date2 = null;
                if(!date.isEmpty()){
                    date+="T00:00:00";
                    date2 = LocalDateTime.parse(date);
                } */


                // Ordina i commenti in base al parametro di ordinamento
                if ("newest".equals(sort)) {
                    comments.sort((e1, e2) -> ((Date) e2.get("c_date")).compareTo((Date) e1.get("c_date")));
                    replies.sort((e1, e2) -> ((Date) e2.get("c_date")).compareTo((Date) e1.get("c_date")));
                } else if ("oldest".equals(sort)) {
                    comments.sort((e1, e2) -> ((Date) e1.get("c_date")).compareTo((Date) e2.get("c_date")));
                    replies.sort((e1, e2) -> ((Date) e1.get("c_date")).compareTo((Date) e2.get("c_date")));
                } else if ("mostreplies".equals(sort)) {
                    comments.sort((e1, e2) -> Long.compare((Long) e2.get("reply_count"), (Long) e1.get("reply_count")));
                    //replies.sort((e1, e2) -> Long.compare((Long) e2.get("reply_count"), (Long) e1.get("reply_count")));
                } else if ("leastreplies".equals(sort)) {
                    comments.sort((e1, e2) -> Long.compare((Long) e1.get("reply_count"), (Long) e2.get("reply_count")));
                    //replies.sort((e1, e2) -> Long.compare((Long) e1.get("reply_count"), (Long) e2.get("reply_count")));
                }



                StringBuilder html = new StringBuilder();
                html.append("<html>");
                    html.append("<head><title>Animaldex Comment</title><style> .comment {background-color: green; color: white; } .reply {background-color: red; color: white; } body{\r\n" + //
                                                    "    background-color: #000000;\r\n" + //
                                                    "    background-position: center;\r\n" + //
                                                    "    background-size: cover;\r\n" + //
                                                    "    background-attachment: fixed;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "/* form */\r\n" + //
                                                    "form{\r\n" + //
                                                    "    color:mediumslateblue;\r\n" + //
                                                    "    text-align: center;\r\n" + //
                                                    "    justify-content: center;\r\n" + //
                                                    "    font-size: 30px;\r\n" + //
                                                    "    font-weight: normal;\r\n" + //
                                                    "    font-family: 'Lucida Sans', 'Lucida Sans Regular', 'Lucida Grande', 'Lucida Sans Unicode', Geneva, Verdana, sans-serif;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "/* back button */\r\n" + //
                                                    "\r\n" + //
                                                    ".back-button {\r\n" + //
                                                    "    position: absolute;\r\n" + //
                                                    "    top: 10px;\r\n" + //
                                                    "    left: 10px;\r\n" + //
                                                    "    background-color: #007bff;\r\n" + //
                                                    "    color: white;\r\n" + //
                                                    "    border: none;\r\n" + //
                                                    "    border-radius: 5px;\r\n" + //
                                                    "    padding: 10px 20px;\r\n" + //
                                                    "    font-size: 16px;\r\n" + //
                                                    "    cursor: pointer;\r\n" + //
                                                    "    transition: background-color 0.3s;\r\n" + //
                                                    "}\r\n" + //
                                                    ".back-button:hover {\r\n" + //
                                                    "    background-color: #0056b3;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "/* bouns points */\r\n" + //
                                                    ".bonus-points {\r\n" + //
                                                    "    color: green;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "/*caratteri*/\r\n" + //
                                                    "hr {\r\n" + //
                                                    "    color:aliceblue;\r\n" + //
                                                    "    border-top: 2px dotted;\r\n" + //
                                                    "    width:50%;\r\n" + //
                                                    "    margin-left: auto;\r\n" + //
                                                    "    margin-right: auto;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "div.descrizione {\r\n" + //
                                                    "\r\n" + //
                                                    "    text-align: center;\r\n" + //
                                                    "    color:aliceblue;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "\r\n" + //
                                                    "a {\r\n" + //
                                                    "    text-decoration: none;\r\n" + //
                                                    "    font-size: 18px;\r\n" + //
                                                    "    color: skyblue;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "h1{\r\n" + //
                                                    "    color: white;\r\n" + //
                                                    "    font-size: 30px;\r\n" + //
                                                    "    font-weight: bold;\r\n" + //
                                                    "    text-align: center;\r\n" + //
                                                    "}\r\n" + //
                                                    "/*fine caratteri*/\r\n" + //
                                                    "\r\n" + //
                                                    "/* top */\r\n" + //
                                                    "#top {\r\n" + //
                                                    "    top: 10;\r\n" + //
                                                    "    text-align: center; \r\n" + //
                                                    "    background-color: #f8f8f8; \r\n" + //
                                                    "    width: 100%; \r\n" + //
                                                    "    padding: 10px 0; \r\n" + //
                                                    "    box-shadow: 0 2px 4px rgba(53, 54, 52, 0); \r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "#top a {\r\n" + //
                                                    "    margin: 0 15px; \r\n" + //
                                                    "    text-decoration: none; \r\n" + //
                                                    "    color: #333; \r\n" + //
                                                    "    font-weight: bold; \r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "#top a:hover {\r\n" + //
                                                    "    color: #007bff; \r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "/* table */\r\n" + //
                                                    "\r\n" + //
                                                    "table {\r\n" + //
                                                    "    width: 100%;\r\n" + //
                                                    "    border-collapse: collapse;\r\n" + //
                                                    "    margin: 20px auto;\r\n" + //
                                                    "    font-size: 16px;\r\n" + //
                                                    "    font-family: 'Arial', sans-serif;\r\n" + //
                                                    "    box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "table thead {\r\n" + //
                                                    "    background-color: #007bff;\r\n" + //
                                                    "    color: #ffffff;\r\n" + //
                                                    "    text-align: left;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "table thead th {\r\n" + //
                                                    "    padding: 12px 15px;\r\n" + //
                                                    "    text-transform: uppercase;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "table tbody tr {\r\n" + //
                                                    "    border-bottom: 1px solid #dddddd;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "table tbody tr:nth-of-type(odd) {\r\n" + //
                                                    "    background-color: #f3f3f3;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "table tbody td {\r\n" + //
                                                    "    padding: 12px 15px;\r\n" + //
                                                    "    background-color: #e9ecef;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "table tbody td a {\r\n" + //
                                                    "    color: #007bff;\r\n" + //
                                                    "    text-decoration: none;\r\n" + //
                                                    "    font-weight: bold;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "table tbody tr:hover {\r\n" + //
                                                    "    background-color: #e9ecef;\r\n" + //
                                                    "}\r\n" + //
                                                    "\r\n" + //
                                                    "table tbody td .empty{\r\n" + //
                                                    "    background-color: #000000;\r\n" + //
                                                    "}\r\n" + //
                                                    "table tbody td a:hover {\r\n" + //
                                                    "    text-decoration: underline;\r\n" + //
                                                    "    color: #0056b3;\r\n" + //
                                                    "}</style></head>");
                    html.append("<body>");
                    html.append("<h1>Welcome to the Animaldex forum!!</h1>");
                    html.append("<label for='sort'>Sort by:</label>")
                    .append("<select name='sort' id='sort'>")
                    .append("<option value='newest'>Newest</option>")
                    .append("<option value='oldest'>Oldest</option>")
                    .append("<option value='mostreplies'>Most Replies</option>")
                    .append("<option value='leastreplies'>Least Replies</option>")
                    .append("</select>")
                    .append("<a id='sortLink' href=''>Sort</a>");
                    html.append("<script type='text/javascript'>");
                    html.append("document.addEventListener('DOMContentLoaded', function() {");
                    html.append("   var sortSelect = document.getElementById('sort');");
                    html.append("   var sortLink = document.getElementById('sortLink');");
                    html.append("   var userId = '").append(userId).append("';");  // Inserisci il valore reale di userId
                    html.append("   var admin = '").append(admin).append("';");    // Inserisci il valore reale di admin
                    html.append("   function updateSortLink() {");
                    html.append("       var sortValue = sortSelect.value;");
                    html.append("       sortLink.href = 'http://localhost:3000/Forum/' + userId + '/' + admin + '/' + sortValue;");
                    html.append("   }");
                    html.append("   sortSelect.addEventListener('change', updateSortLink);");
                    html.append("   updateSortLink();");  // Inizializza l'href del link
                    html.append("});");
                    html.append("</script>");
                    html.append("<table border='1'>");


                if(!admin) {
                    
                    for (Map<String, Object> comment : comments) {
                            html.append("<tr>")
                            .append("<th class='comment'>Comment</th>")
                            .append("<th class='comment'>Username</th>")
                            .append("<th class='comment'>Date</th>")
                            .append("<th class='comment'>Last Modify</th>")
                            .append("<th class='comment'>Actions</th>")
                            .append("</tr>")
                            .append("<tr>")
                            .append("<td>").append(comment.get("c_content")).append("</td>")
                            .append("<td>").append(comment.get("username")).append("</td>")
                            .append("<td>").append(comment.get("c_date")).append("</td>");
                            if(comment.get("modify_date")==null) {
                                html.append("<td>").append("-").append("</td>");
                            }
                            else {
                                html.append("<td>").append(comment.get("modify_date")).append("</td>");
                            }
                            html.append("<td>");
                            if(comment.get("user_id").equals(userId)) {
                                html.append("<form action='http://localhost:6039/modifyComment' method='post'>")
                                .append("<input type='hidden' name='c_id' value='").append(comment.get("c_id")).append("'>")
                                .append("<input type='hidden' name='user_id' value='").append(userId).append("'>")
                                .append("<input type='hidden' name='admin' value='").append(admin).append("'>")
                                .append("<input type='hidden' name='modify_date' value='").append(LocalDate.now()).append("'>")
                                .append("<input type='hidden' name='sort' value='").append(sort).append("'/>")
                                .append("<textarea name='c_content' rows='4' cols='50' required></textarea><br>")
                                .append("<button type='submit'>Modify comment</button>")
                                .append("</form>")
                                .append("<form action='http://localhost:6039/deleteComment' method='post'>")
                                .append("<input type='hidden' name='c_id' value='").append(comment.get("c_id")).append("'>")
                                .append("<input type='hidden' name='user_id' value='").append(userId).append("'>")
                                .append("<input type='hidden' name='admin' value='").append(admin).append("'>")
                                .append("<input type='hidden' name='sort' value='").append(sort).append("'/>")
                                .append("<button type='submit'>Delete comment</button>")
                                .append("</form>");
                            }
                            //Add reply to comment section
                            html.append("<form action='http://localhost:6039/addReply' method='post'>")
                            .append("<input type='hidden' name='user_id' value='").append(userId).append("'>")
                            .append("<input type='hidden' name='admin' value='").append(admin).append("'>")
                            .append("<textarea name='c_content' rows='4' cols='50' required></textarea><br>")
                            .append("<input type='hidden' name='username' value='").append(username).append("'>")
                            .append("<input type='hidden' name='date' value='").append(LocalDate.now()).append("'>")
                            .append("<input type='hidden' name='c_id' value='").append(comment.get("c_id")).append("'>")
                            .append("<input type='hidden' name='sort' value='").append(sort).append("'/>")
                            .append("<button type='submitReply'> Reply comment</button>")
                            .append("</form>")
                            .append("</td>")
                            .append("</tr>");
                        for (Map<String, Object> reply : replies) {
                            if (reply.get("c_id_orig").equals(comment.get("c_id"))) {
                                html//.append("<td class='empty'></td>") // Empty cell to indent the reply
                                    .append("<table border='1'>")
                                    .append("<tr>")
                                    .append("<th class='reply'>Reply</th>")
                                    .append("<th class='reply'>Username</th>")
                                    .append("<th class='reply'>Date</th>")
                                    .append("<th class='reply'>Last Modify</th>")
                                    .append("<th class='reply'>Actions</th>")
                                    .append("</tr>")
                                    .append("<tr>")
                                    .append("<td>").append(reply.get("c_content")).append("</td>")
                                    .append("<td>").append(reply.get("username")).append("</td>")
                                    .append("<td>").append(reply.get("c_date")).append("</td>");
                                    if(reply.get("modify_date")==null) {
                                        html.append("<td>").append("-").append("</td>");
                                    }
                                    else {
                                        html.append("<td>").append(reply.get("modify_date")).append("</td>");
                                    }
                                    html.append("<td>");
                                    if(reply.get("user_id").equals(userId)) {
                                        html.append("<form action='http://localhost:6039/deleteReply' method='post'>")
                                        .append("<input type='hidden' name='c_id' value='").append(reply.get("c_id_reply")).append("'>")
                                        .append("<input type='hidden' name='user_id' value='").append(userId).append("'>")
                                        .append("<input type='hidden' name='admin' value='").append(admin).append("'>")
                                        .append("<input type='hidden' name='sort' value='").append(sort).append("'/>")
                                        .append("<button type='submit'>Delete reply</button>")
                                        .append("</form>")
                                        .append("<form action='http://localhost:6039/modifyReply' method='post'>")
                                        .append("<input type='hidden' name='c_id' value='").append(reply.get("c_id_reply")).append("'>")
                                        .append("<input type='hidden' name='user_id' value='").append(userId).append("'>")
                                        .append("<input type='hidden' name='admin' value='").append(admin).append("'>")
                                        .append("<input type='hidden' name='modify_date' value='").append(LocalDate.now()).append("'>")
                                        .append("<input type='hidden' name='sort' value='").append(sort).append("'/>")
                                        .append("<textarea name='c_content' rows='4' cols='50' required></textarea><br>")
                                        .append("<button type='submit'>Modify reply</button>")
                                        .append("</form>");
                                    }
                                    html.append("</td>")
                                    .append("</tr>")
                                    .append("</td>")
                                    .append("</tr>");
                            }
                        }
                        html.append("<br><br>");
                    }

                    html.append("</table>");
                }
                else {

                    for (Map<String, Object> comment : comments) {
                            html.append("<tr>")
                            .append("<th class='comment'>Comment</th>")
                            .append("<th class='comment'>Username</th>")
                            .append("<th class='comment'>Date</th>")
                            .append("<th class='comment'>Last Modify</th>")
                            .append("<th class='comment'>Actions</th>")
                            .append("</tr>")
                            .append("<tr>")
                            .append("<td>").append(comment.get("c_content")).append("</td>")
                            .append("<td>").append(comment.get("username")).append("</td>")
                            .append("<td>").append(comment.get("c_date")).append("</td>");
                            if(comment.get("modify_date")==null) {
                                html.append("<td>").append("-").append("</td>");
                            }
                            else {
                                html.append("<td>").append(comment.get("modify_date")).append("</td>");
                            }
                            html.append("<td>");
                            if(comment.get("user_id").equals(userId)) {
                                html.append("<form action='http://localhost:6039/modifyComment' method='post'>")
                                .append("<input type='hidden' name='c_id' value='").append(comment.get("c_id")).append("'>")
                                .append("<input type='hidden' name='user_id' value='").append(userId).append("'>")
                                .append("<input type='hidden' name='admin' value='").append(admin).append("'>")
                                .append("<input type='hidden' name='modify_date' value='").append(LocalDate.now()).append("'>")
                                .append("<input type='hidden' name='sort' value='").append(sort).append("'/>")
                                .append("<textarea name='c_content' rows='4' cols='50' required></textarea><br>")
                                .append("<button type='submit'>Modify comment</button>")
                                .append("</form>");
                            }
                            html.append("<form action='http://localhost:6039/deleteComment' method='post'>")
                            .append("<input type='hidden' name='c_id' value='").append(comment.get("c_id")).append("'>")
                            .append("<input type='hidden' name='user_id' value='").append(userId).append("'>")
                            .append("<input type='hidden' name='admin' value='").append(admin).append("'>")
                            .append("<input type='hidden' name='sort' value='").append(sort).append("'/>")
                            .append("<button type='submit'>Delete comment</button>")
                            .append("</form>");
                            //Add reply to comment section
                            html.append("<form action='http://localhost:6039/addReply' method='post'>")
                            .append("<input type='hidden' name='user_id' value='").append(userId).append("'>")
                            .append("<input type='hidden' name='admin' value='").append(admin).append("'>")
                            .append("<textarea name='c_content' rows='4' cols='50' required></textarea><br>")
                            .append("<input type='hidden' name='username' value='").append(username).append("'>")
                            .append("<input type='hidden' name='date' value='").append(LocalDate.now()).append("'>")
                            .append("<input type='hidden' name='c_id' value='").append(comment.get("c_id")).append("'>")
                            .append("<input type='hidden' name='sort' value='").append(sort).append("'/>")
                            .append("<button type='submitReply'> Reply comment </button>")
                            .append("</form>")
                            .append("</td>")
                            .append("</tr>");
                            

                            html.append("</td>")
                            .append("</tr>");
                        for (Map<String, Object> reply : replies) {
                            if (reply.get("c_id_orig").equals(comment.get("c_id"))) {
                                    html.append("<table border='1'>")
                                    .append("<tr>")
                                    .append("<th class='reply'>Reply</th>")
                                    .append("<th class='reply'>Username</th>")
                                    .append("<th class='reply'>Date</th>")
                                    .append("<th class='reply'>Last Modify</th>")
                                    .append("<th class='reply'>Actions</th>")
                                    .append("</tr>")
                                    .append("<tr>")
                                    .append("<td>").append(reply.get("c_content")).append("</td>")
                                    .append("<td>").append(reply.get("username")).append("</td>")
                                    .append("<td>").append(reply.get("c_date")).append("</td>");
                                    if(reply.get("modify_date")==null) {
                                        html.append("<td>").append("-").append("</td>");
                                    }
                                    else {
                                        html.append("<td>").append(reply.get("modify_date")).append("</td>");
                                    }
                                    html.append("<td>")
                                    .append("<form action='http://localhost:6039/deleteReply' method='post'>")
                                    .append("<input type='hidden' name='c_id' value='").append(reply.get("c_id_reply")).append("'>")
                                    .append("<input type='hidden' name='user_id' value='").append(userId).append("'>")
                                    .append("<input type='hidden' name='admin' value='").append(admin).append("'>")
                                    .append("<input type='hidden' name='sort' value='").append(sort).append("'/>")
                                    .append("<button type='submit'>Delete reply</button>")
                                    .append("</form>");
                                    if(reply.get("user_id").equals(userId)) {
                                        html.append("<form action='http://localhost:6039/modifyReply' method='post'>")
                                        .append("<input type='hidden' name='c_id' value='").append(reply.get("c_id_reply")).append("'>")
                                        .append("<input type='hidden' name='user_id' value='").append(userId).append("'>")
                                        .append("<input type='hidden' name='admin' value='").append(admin).append("'>")
                                        .append("<input type='hidden' name='modify_date' value='").append(LocalDate.now()).append("'>")
                                        .append("<textarea name='c_content' rows='4' cols='50' required></textarea><br>")
                                        .append("<input type='hidden' name='sort' value='").append(sort).append("'/>")
                                        .append("<button type='submit'>Modify reply</button>")
                                        .append("</form>");
                                    }
                                    html.append("</td>")
                                    .append("</tr>")
                                    .append("</td>")
                                    .append("</tr>")
                                    .append("</td>")
                                    .append("</tr>");
                            }
                        }
                    }

                    html.append("</table>");
                }
                // Aggiungi textarea e bottone per scrivere un commento e aggiungerlo al db
                html.append("<h2>Add a new comment</h2>")
                .append("<form action='http://localhost:6039/addComment' method='post'>")
                .append("<input type='hidden' name='user_id' value='").append(userId).append("'>")
                .append("<input type='hidden' name='admin' value='").append(admin).append("'>")
                .append("<textarea name='c_content' rows='4' cols='50' required></textarea><br>")
                .append("<input type='hidden' name='username' value='").append(username).append("'>")
                .append("<input type='hidden' name='date' value='").append(LocalDate.now()).append("'>")
                .append("<input type='hidden' name='sort' value='").append(sort).append("'/>")
                .append("<button type='submitComment'> Add Comment </button>")
                .append("</form>");

                html.append("<form>")
                .append("<button type='button' onclick='redirectToPersonalPage()'> Return to Personal Page </button>")
                .append("</form>")
                .append("<script type='text/javascript' language='javascript'>")
                .append("function redirectToPersonalPage() {")
                .append("window.location.href = 'http://localhost:3000/Redirect/").append(Integer.toString(userId)).append("';")
                .append("}")
                .append("</script>");

                html.append("</body>");
                html.append("</html>");

                ObjectMapper mapper = new ObjectMapper();
                ObjectNode responseJson = mapper.createObjectNode();

                responseJson.put("forumResponse", html.toString());
                

                String jsonResponse = mapper.writeValueAsString(responseJson);
                return ResponseEntity.ok(jsonResponse);
                
                //return html.toString();
        }
    
    //OAUTH---------------------------------------------------------------------------------------------

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
