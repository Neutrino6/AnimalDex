package it.uniroma1.animaldex;

import lombok.Data;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Date;
import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;

import java.util.ArrayList;
import java.util.Base64;
import java.time.LocalDateTime;
import org.json.JSONObject;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.servlet.view.RedirectView;
import org.thymeleaf.TemplateEngine;

import org.thymeleaf.context.Context;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@RestController
public class CertificatesController {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private TemplateEngine templateEngine;

    @RequestMapping("/{user_id}/certificates")
    public String certificates(@PathVariable int user_id,@CookieValue(value = "authCookie", defaultValue = "") String authCookieValue) {
        int check=isValidAuthCookie(authCookieValue,user_id);
        if(check==0){
            Context context = new Context();
            //this is an example if you want to add same variable to your context to add in the template
            context.setVariable("error", "You are not authorized to perform this action.");
            context.setVariable("redirectUrl","http://localhost:3000/LoginUser.html");
            // first argument of processe is the name of the template you want to use
            String html = templateEngine.process("errorPage", context);
            return html;
        }

        Context context = new Context();
        if(check==2){
            context.setVariable("googleDriveAuthUrl", "http://localhost:8080/auth/google");
        }
        //this is an example if you want to add same variable to your context to add in the template
        //context.setVariable("name", "John Doe");
        // first argument of processe is the name of the template you want to use
        context.setVariable("userId", ""+user_id+"");
        context.setVariable("link1", "/"+user_id+"/certificates/list");
        context.setVariable("link2", "/"+user_id+"/certificates/animals");
        String html = templateEngine.process("certificates", context);
        return html;
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    // this function returns:
    // 0 if the cookie is not valid
    // 1 if the cookie is valid and obtained with the login
    // 2 if the cookie is valid and obtained with google oauth
    private int isValidAuthCookie(String cookieValue,int userId) {

        String expectedLoginCookieValue = sha256("LOGIN:" + userId);
        String expectedOauthCookieValue = sha256("GOOGLE_OAUTH:" + userId);
        if(cookieValue.equals(expectedOauthCookieValue)) return 2;
        if(cookieValue.equals(expectedLoginCookieValue)) return 1;
        return 0;
    }

    @RequestMapping(value = "/{user_id}/certificates/upload", method = RequestMethod.POST)
    public String upload(@PathVariable int user_id,@CookieValue(value = "authCookie", defaultValue = "") String authCookieValue,@RequestParam("data") String data, @RequestParam("fileInput") MultipartFile fileInput) {
        
        // Check if file is empty
        if (fileInput.isEmpty()) {
            return "Error: File is empty <br> <a href='/"+user_id+"/certificates'>Click here to insert a new certificate</a>";
        }
        
        // Process the fileInput here
        String fileName = fileInput.getOriginalFilename();
        
        // Convert file to base64 for displaying in HTML
        String base64Image = null;
        byte[] fileBytes;
        int animalId=0;
        try {
            fileBytes = fileInput.getBytes();
            base64Image = Base64.getEncoder().encodeToString(fileBytes);
        } catch (IOException e) {
            e.printStackTrace();
            return "Error processing file <br> <a href='http://localhost:7777/"+user_id+"/certificates'>Click here to insert a new certificate</a>";
        }
        
        // If data is UNRECOGNIZED, handle the fileInput
        if (data.equals("UNRECOGNIZED")) {
            // Return a response message or perform further actions based on the file content
            return "File unrecognized. <a href='http://localhost:7777/certificates'>Click here to insert a new certificate</a>";
        } else {

            System.out.println("Animal name: " + data);

            //Send request to centralServer for validation
        
            // Create RequestBody
            String jsonBody = "{\"animalName\":\""+data+"\"}";
            
            // Create HTTP request
            HttpPost request = new HttpPost("http://host.docker.internal:6039/newCertificate?user_id="+user_id);  //communication between different containers
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonBody, "UTF-8"));

            try (CloseableHttpClient client = HttpClients.createDefault()) {
                CloseableHttpResponse response = client.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity);

            
                System.out.println("Status code: " + statusCode);
                System.out.println("Response body: " + responseBody);

                JSONObject jsonObject = new JSONObject(responseBody);

                animalId = jsonObject.getInt("animal_id");
                System.out.println("Animal id:"+animalId);
                if(animalId>0){
                } else {
                    System.out.println("No number found following 'animal_id='.");
                    return "No animal found with such a name <br> <a href='http://localhost:7777/"+user_id+"/certificates'>Click here to insert a new certificate</a>";
                }
            
                // Add response validation logic here if needed
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(); // Print detailed stack trace for debugging
            }

            LocalDateTime currentTime = LocalDateTime.now();
            
            //query to check if already exists a certificate about that animal
            MapSqlParameterSource source1 = new MapSqlParameterSource().addValue("animal_id", animalId);
            source1.addValue("user_id", user_id );
            String checkCertificate= "SELECT count(*) from certification where animal_id = :animal_id and user_id = :user_id";
            Integer count = jdbcTemplate.queryForObject(checkCertificate, source1,Integer.class);
            if(count!=null && count>0 ){
                //update the last certificate
                String updateCertificate= "UPDATE certification SET cert_date = :cert_date where animal_id = :animal_id and user_id = :user_id";
                MapSqlParameterSource source3 = new MapSqlParameterSource().addValue("animal_id", animalId);
                source3.addValue("user_id", user_id );
                source3.addValue("cert_date", currentTime);
                jdbcTemplate.update(updateCertificate, source3);
                // Return HTML with image tag to display the uploaded image
                return "<img src='data:image/jpeg;base64," + base64Image + "' alt='" + fileName + "'>" +
                "<p>File recognized as " + data + ". and has been updated at" + currentTime+ 
                "<a href='http://localhost:7777/"+user_id+"/certificates'>Click here to insert a new certificate</a> <br></p>";
            }
            else{
                MapSqlParameterSource source2 = new MapSqlParameterSource().addValue("cert_image",fileBytes);
                source2.addValue("animal_id", animalId );
                source2.addValue("user_id", user_id );
                source2.addValue("cert_date", currentTime);


                String InsertCertificate = "INSERT INTO certification (cert_image, animal_id, user_id, cert_date) VALUES (:cert_image, :animal_id, :user_id, :cert_date)";
                jdbcTemplate.update(InsertCertificate, source2);

                // Return HTML with image tag to display the uploaded image
                return "<img src='data:image/jpeg;base64," + base64Image + "' alt='" + fileName + "'>" +
                "<p>File recognized as " + data + ". <a href='http://localhost:7777/"+user_id+"/certificates'>Click here to insert a new certificate</a> <br></p>";
            }
        }
    }

    @RequestMapping("/{user_id}/certificates/list")
    String certificatesList(@PathVariable int user_id,@CookieValue(value = "authCookie", defaultValue = "") String authCookieValue) {
        int check=isValidAuthCookie(authCookieValue,user_id);
        if(check==0){
            Context context = new Context();
            //this is an example if you want to add same variable to your context to add in the template
            context.setVariable("error", "You are not authorized to perform this action.");
            context.setVariable("redirectUrl","http://localhost:3000/LoginUser.html");
            // first argument of processe is the name of the template you want to use
            String html = templateEngine.process("errorPage", context);
            return html;
        }
        
        Context context = new Context();
        MapSqlParameterSource getCert=new MapSqlParameterSource();
        getCert.addValue("user_id", user_id );
        String GetCertificates= "SELECT a_name, cert_date, details, regions from certification JOIN animal on animal_id = a_id where user_id=:user_id"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, getCert);
        
        List<String> AnimalsName = new ArrayList<String>();
        List<String> AnimalsDescription = new ArrayList<String>();
        List<String> AnimalsRegions = new ArrayList<String>();
        List<Date> CertificatesDate = new ArrayList<Date>();

        for (Map<String, Object> row : rows) {
            String name = (String) row.get("a_name");
            Date certDate = (Date) row.get("cert_date");
            String details = (String) row.get("details");
            String region = (String) row.get("regions");
            
            AnimalsName.add(name);
            AnimalsDescription.add(details);
            AnimalsRegions.add(region);
            CertificatesDate.add(certDate);
            
        }
        context.setVariable("link1", "/"+user_id+"/certificates");
        context.setVariable("link2", "/"+user_id+"/certificates/list/filter");
        context.setVariable("animalsNameRegistered", AnimalsName);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescription);
        context.setVariable("animalsRegionsRegistered", AnimalsRegions);
        context.setVariable("certificateDate", CertificatesDate);

        String html = templateEngine.process("personaltable", context);
        return html;
    }

    @RequestMapping(value="/{user_id}/certificates/list/filter", method = RequestMethod.POST)
    String certificatesFilteredList(@PathVariable int user_id,@CookieValue(value = "authCookie", defaultValue = "") String authCookieValue,@RequestParam("filter") String regione) {
        int check=isValidAuthCookie(authCookieValue,user_id);
        if(check==0){
            Context context = new Context();
            //this is an example if you want to add same variable to your context to add in the template
            context.setVariable("error", "You are not authorized to perform this action.");
            context.setVariable("redirectUrl","http://localhost:3000/LoginUser.html");
            // first argument of processe is the name of the template you want to use
            String html = templateEngine.process("errorPage", context);
            return html;
        }
        
        Context context = new Context();
        MapSqlParameterSource filterSearch = new MapSqlParameterSource()
                                                    .addValue("filter", regione)
                                                    .addValue("user_id", user_id);
        String GetCertificates= "SELECT a_name, cert_date, details, regions from certification JOIN animal on animal_id = a_id where user_id=:user_id AND regions ILIKE concat('%',:filter ,'%')"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, filterSearch);
        
        if(rows.isEmpty()){
            context.setVariable("link1", "/"+user_id+"/certificates/list");
            context.setVariable("link2", "/"+user_id+"/certificates/list/filter");
            context.setVariable("error", "No registered animal in that region");
            String html = templateEngine.process("personaltableError", context);
            return html;
        }

        List<String> AnimalsName = new ArrayList<String>();
        List<String> AnimalsDescription = new ArrayList<String>();
        List<String> AnimalsRegions = new ArrayList<String>();
        List<Date> CertificatesDate = new ArrayList<Date>();

        for (Map<String, Object> row : rows) {
            String name = (String) row.get("a_name");
            Date certDate = (Date) row.get("cert_date");
            String details = (String) row.get("details");
            String region = (String) row.get("regions");
            
            AnimalsName.add(name);
            AnimalsDescription.add(details);
            AnimalsRegions.add(region);
            CertificatesDate.add(certDate);
            
        }
        context.setVariable("link1", "/"+user_id+"/certificates/list");
        context.setVariable("link2", "/"+user_id+"/certificates/list/filter");
        context.setVariable("animalsNameRegistered", AnimalsName);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescription);
        context.setVariable("animalsRegionsRegistered", AnimalsRegions);
        context.setVariable("certificateDate", CertificatesDate);

        String html = templateEngine.process("personaltablefiltered", context);
        return html;
    }

    @RequestMapping("/{user_id}/certificates/image")
    String ShowImage(@PathVariable int user_id,@CookieValue(value = "authCookie", defaultValue = "") String authCookieValue,@RequestParam("animal") String name) {
        int check=isValidAuthCookie(authCookieValue,user_id);
        if(check==0){
            Context context = new Context();
            //this is an example if you want to add same variable to your context to add in the template
            context.setVariable("error", "You are not authorized to perform this action.");
            context.setVariable("redirectUrl","http://localhost:3000/LoginUser.html");
            // first argument of processe is the name of the template you want to use
            String html = templateEngine.process("errorPage", context);
            return html;
        }
        
        //Shows the last uploaded image for the selected certificate
        String GetImage= "SELECT cert_image from certification JOIN animal on animal_id=a_id where a_name=:name and user_id=:user_id";
        MapSqlParameterSource source7 = new MapSqlParameterSource().addValue("name", name).addValue("user_id", user_id);
        byte[] imageBytes = jdbcTemplate.queryForObject(GetImage, source7, byte[].class);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        return "<img src='data:image/jpeg;base64," + base64Image + "' alt='" + name + "'>"+
        "<br> <a href='/"+user_id+"/certificates'> Go back to upload certificates </a> <br>"+
        "<a href='../certificates/list'> Go back to your certificates</a>";
    }
    
    @RequestMapping("/{user_id}/certificates/animals")
    String AnimalsList(@PathVariable int user_id,@CookieValue(value = "authCookie", defaultValue = "") String authCookieValue) {
        int check=isValidAuthCookie(authCookieValue,user_id);
        if(check==0){
            Context context = new Context();
            //this is an example if you want to add same variable to your context to add in the template
            context.setVariable("error", "You are not authorized to perform this action.");
            context.setVariable("redirectUrl","http://localhost:3000/LoginUser.html");
            // first argument of processe is the name of the template you want to use
            String html = templateEngine.process("errorPage", context);
            return html;
        }
        
        Context context = new Context();
        String GetCertificates= "SELECT a_id, a_name, details, regions from animal"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, new MapSqlParameterSource());

        String Getids= "SELECT animal_id from certification where user_id=:user_id"; //user=999 to be replaced by current_user
        List<Map<String, Object>> ids = jdbcTemplate.queryForList(Getids, new MapSqlParameterSource().addValue("user_id", user_id));

        List<String> AnimalsName = new ArrayList<String>();
        List<String> AnimalsNameRegistered = new ArrayList<String>();
        List<String> AnimalsDescription = new ArrayList<String>();
        List<String> AnimalsDescriptionRegistered = new ArrayList<String>();
        List<String> AnimalsRegions = new ArrayList<String>();
        List<String> AnimalsRegionsRegistered = new ArrayList<String>();

        // Popolamento delle liste con i dati dal database
        for (Map<String, Object> result : rows) {
            Integer id=(Integer) result.get("a_id");
            String name = (String) result.get("a_name");
            String region = (String) result.get("regions");
            String detail = (String) result.get("details");

            Boolean isPresent=false;
            for (Map<String, Object> animalids : ids) {                 // checks if the user has registered the animal
                Integer animalid=(Integer) animalids.get("animal_id");
                if(animalid.equals(id)){
                    isPresent=true;
                    break;
                } 
            }
            if(isPresent){    
                AnimalsNameRegistered.add(name);
                AnimalsDescriptionRegistered.add(detail);
                AnimalsRegionsRegistered.add(region);
            }
            else{
                AnimalsName.add(name);
                AnimalsDescription.add(detail);
                AnimalsRegions.add(region);
            }
        }

        context.setVariable("link1", "/"+user_id+"/certificates");
        context.setVariable("link2", "/"+user_id+"/certificates/animals/search");
        context.setVariable("animalsNameRegistered", AnimalsNameRegistered);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescriptionRegistered);
        context.setVariable("animalsRegionsRegistered", AnimalsRegionsRegistered);
        context.setVariable("animalsName", AnimalsName);


        String html = templateEngine.process("table", context);
        return html;
    }

    @RequestMapping("/{user_id}/certificates/animals/increasingOrder")
    String AnimalsListIncOrder(@PathVariable int user_id,@CookieValue(value = "authCookie", defaultValue = "") String authCookieValue) {
        int check=isValidAuthCookie(authCookieValue,user_id);
        if(check==0){
            Context context = new Context();
            //this is an example if you want to add same variable to your context to add in the template
            context.setVariable("error", "You are not authorized to perform this action.");
            context.setVariable("redirectUrl","http://localhost:3000/LoginUser.html");
            // first argument of processe is the name of the template you want to use
            String html = templateEngine.process("errorPage", context);
            return html;
        }
        
        Context context = new Context();
        String GetCertificates= "SELECT a_id, a_name, details, regions from animal order by a_name"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, new MapSqlParameterSource());

        String Getids= "SELECT animal_id from certification where user_id=:user_id"; //user=999 to be replaced by current_user
        List<Map<String, Object>> ids = jdbcTemplate.queryForList(Getids, new MapSqlParameterSource().addValue("user_id", user_id));

        List<String> AnimalsName = new ArrayList<String>();
        List<String> AnimalsNameRegistered = new ArrayList<String>();
        List<String> AnimalsDescription = new ArrayList<String>();
        List<String> AnimalsDescriptionRegistered = new ArrayList<String>();
        List<String> AnimalsRegions = new ArrayList<String>();
        List<String> AnimalsRegionsRegistered = new ArrayList<String>();

        // Popolamento delle liste con i dati dal database
        for (Map<String, Object> result : rows) {
            Integer id=(Integer) result.get("a_id");
            String name = (String) result.get("a_name");
            String region = (String) result.get("regions");
            String detail = (String) result.get("details");

            Boolean isPresent=false;
            for (Map<String, Object> animalids : ids) {                 // checks if the user has registered the animal
                Integer animalid=(Integer) animalids.get("animal_id");
                if(animalid.equals(id)){
                    isPresent=true;
                    break;
                } 
            }
            if(isPresent){    
                AnimalsNameRegistered.add(name);
                AnimalsDescriptionRegistered.add(detail);
                AnimalsRegionsRegistered.add(region);
            }
            else{
                AnimalsName.add(name);
                AnimalsDescription.add(detail);
                AnimalsRegions.add(region);
            }
        }

        context.setVariable("link1", "/"+user_id+"/certificates");
        context.setVariable("link2", "/"+user_id+"/certificates/animals/search");
        context.setVariable("animalsNameRegistered", AnimalsNameRegistered);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescriptionRegistered);
        context.setVariable("animalsRegionsRegistered", AnimalsRegionsRegistered);
        context.setVariable("animalsName", AnimalsName);


        String html = templateEngine.process("tableinc", context);
        return html;
    }

    @RequestMapping("/{user_id}/certificates/animals/decreasingOrder")
    String AnimalsListDecOrder(@PathVariable int user_id,@CookieValue(value = "authCookie", defaultValue = "") String authCookieValue) {
        int check=isValidAuthCookie(authCookieValue,user_id);
        if(check==0){
            Context context = new Context();
            //this is an example if you want to add same variable to your context to add in the template
            context.setVariable("error", "You are not authorized to perform this action.");
            context.setVariable("redirectUrl","http://localhost:3000/LoginUser.html");
            // first argument of processe is the name of the template you want to use
            String html = templateEngine.process("errorPage", context);
            return html;
        }
        Context context = new Context();
        String GetCertificates= "SELECT a_id, a_name, details, regions from animal order by a_name DESC"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, new MapSqlParameterSource());

        String Getids= "SELECT animal_id from certification where user_id=:user_id"; //user=999 to be replaced by current_user
        List<Map<String, Object>> ids = jdbcTemplate.queryForList(Getids, new MapSqlParameterSource().addValue("user_id", user_id));

        List<String> AnimalsName = new ArrayList<String>();
        List<String> AnimalsNameRegistered = new ArrayList<String>();
        List<String> AnimalsDescription = new ArrayList<String>();
        List<String> AnimalsDescriptionRegistered = new ArrayList<String>();
        List<String> AnimalsRegions = new ArrayList<String>();
        List<String> AnimalsRegionsRegistered = new ArrayList<String>();

        // Popolamento delle liste con i dati dal database
        for (Map<String, Object> result : rows) {
            Integer id=(Integer) result.get("a_id");
            String name = (String) result.get("a_name");
            String region = (String) result.get("regions");
            String detail = (String) result.get("details");

            Boolean isPresent=false;
            for (Map<String, Object> animalids : ids) {                 // checks if the user has registered the animal
                Integer animalid=(Integer) animalids.get("animal_id");
                if(animalid.equals(id)){
                    isPresent=true;
                    break;
                } 
            }
            if(isPresent){    
                AnimalsNameRegistered.add(name);
                AnimalsDescriptionRegistered.add(detail);
                AnimalsRegionsRegistered.add(region);
            }
            else{
                AnimalsName.add(name);
                AnimalsDescription.add(detail);
                AnimalsRegions.add(region);
            }
        }

        context.setVariable("link1", "/"+user_id+"/certificates");
        context.setVariable("link2", "/"+user_id+"/certificates/animals/search");
        context.setVariable("animalsNameRegistered", AnimalsNameRegistered);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescriptionRegistered);
        context.setVariable("animalsRegionsRegistered", AnimalsRegionsRegistered);
        context.setVariable("animalsName", AnimalsName);


        String html = templateEngine.process("tableinv", context);
        return html;
    }

    @RequestMapping(value="/{user_id}/certificates/animals/search", method = RequestMethod.POST)
    String AnimalSearch(@PathVariable int user_id,@CookieValue(value = "authCookie", defaultValue = "") String authCookieValue,@RequestParam("searchAnimal") String animalsearch) {
        int check=isValidAuthCookie(authCookieValue,user_id);
        if(check==0){
            Context context = new Context();
            //this is an example if you want to add same variable to your context to add in the template
            context.setVariable("error", "You are not authorized to perform this action.");
            context.setVariable("redirectUrl","http://localhost:3000/LoginUser.html");
            // first argument of processe is the name of the template you want to use
            String html = templateEngine.process("errorPage", context);
            return html;
        }
        Context context = new Context();
        MapSqlParameterSource sourceSearch = new MapSqlParameterSource().addValue("animalName", animalsearch);
        String GetCertificates= "SELECT a_id, a_name, details, regions from animal where a_name ILIKE concat('%',:animalName ,'%')"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, sourceSearch);

        if(rows.isEmpty()){
            context.setVariable("error", "No such animal found");
            context.setVariable("link1", "/"+user_id+"/certificates/animals");
            context.setVariable("link2", "/"+user_id+"/certificates/animals/search");
            String html = templateEngine.process("tableError", context);
            return html;
        }

        String Getids= "SELECT animal_id from certification where user_id=:user_id"; //user=999 to be replaced by current_user
        List<Map<String, Object>> ids = jdbcTemplate.queryForList(Getids, new MapSqlParameterSource().addValue("user_id", user_id));

        List<String> AnimalsName = new ArrayList<String>();
        List<String> AnimalsNameRegistered = new ArrayList<String>();
        List<String> AnimalsDescription = new ArrayList<String>();
        List<String> AnimalsDescriptionRegistered = new ArrayList<String>();
        List<String> AnimalsRegions = new ArrayList<String>();
        List<String> AnimalsRegionsRegistered = new ArrayList<String>();


        // Popolamento delle liste con i dati dal database
        for (Map<String, Object> result : rows) {
            Integer id=(Integer) result.get("a_id");
            String name = (String) result.get("a_name");
            String region = (String) result.get("regions");
            String detail = (String) result.get("details");

            Boolean isPresent=false;
            for (Map<String, Object> animalids : ids) {                 // checks if the user has registered the animal
                Integer animalid=(Integer) animalids.get("animal_id");
                if(animalid.equals(id)){
                    isPresent=true;
                    break;
                } 
            }
            if(isPresent){    
                AnimalsNameRegistered.add(name);
                AnimalsDescriptionRegistered.add(detail);
                AnimalsRegionsRegistered.add(region);
            }
            else{
                AnimalsName.add(name);
                AnimalsDescription.add(detail);
                AnimalsRegions.add(region);
            }
        }

        context.setVariable("link1", "/"+user_id+"/certificates/animals");
        context.setVariable("link2", "/"+user_id+"/certificates/animals/search");
        context.setVariable("animalsNameRegistered", AnimalsNameRegistered);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescriptionRegistered);
        context.setVariable("animalsRegionsRegistered", AnimalsRegionsRegistered);
        context.setVariable("animalsName", AnimalsName);


        String html = templateEngine.process("tableSearch", context);
        return html;
    }
}

