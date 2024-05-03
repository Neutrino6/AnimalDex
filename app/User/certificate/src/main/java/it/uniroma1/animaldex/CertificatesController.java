package it.uniroma1.animaldex;

import lombok.Data;

import java.io.IOException;
import java.sql.Date;
import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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

import org.springframework.web.servlet.ModelAndView;
import org.thymeleaf.TemplateEngine;

import org.thymeleaf.context.Context;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

@RestController
public class CertificatesController {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private TemplateEngine templateEngine;

    @RequestMapping("/certificates")
    public String certificates() {
        Context context = new Context();
        //this is an example if you want to add same variable to your context to add in the template
        //context.setVariable("name", "John Doe");
        // first argument of processe is the name of the template you want to use
        String html = templateEngine.process("certificates", context);
        return html;
    }

    @RequestMapping(value = "/certificates/upload", method = RequestMethod.POST)
    public String upload(@RequestParam("data") String data, @RequestParam("fileInput") MultipartFile fileInput) {
        // Check if file is empty
        if (fileInput.isEmpty()) {
            return "Error: File is empty <br> <a href='/certificates'>Click here to insert a new certificate</a>";
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
            return "Error processing file <br> <a href='/certificates'>Click here to insert a new certificate</a>";
        }
        
        // If data is UNRECOGNIZED, handle the fileInput
        if (data.equals("UNRECOGNIZED")) {
            // Return a response message or perform further actions based on the file content
            return "File unrecognized. <a href='/certificates'>Click here to insert a new certificate</a>";
        } else {

            System.out.println("Animal name: " + data);

            //Send request to centralServer for validation
        
            // Create RequestBody
            String jsonBody = "{\"animalName\":\""+data+"\"}";
            
            // Create HTTP request
            HttpPost request = new HttpPost("http://host.docker.internal:6039/newCertificate");  //communication between different containers
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
                    return "No animal found with such a name <br> <a href='/certificates'>Click here to insert a new certificate</a>";
                }
            
                // Add response validation logic here if needed
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(); // Print detailed stack trace for debugging
            }

            LocalDateTime currentTime = LocalDateTime.now();
            
            //query to check if already exists a certificate about that animal
            MapSqlParameterSource source1 = new MapSqlParameterSource().addValue("animal_id", animalId);
            source1.addValue("user_id", 999 );
            String checkCertificate= "SELECT count(*) from certification where animal_id = :animal_id and user_id = :user_id";
            Integer count = jdbcTemplate.queryForObject(checkCertificate, source1,Integer.class);
            if(count!=null && count>0 ){
                //update the last certificate
                String updateCertificate= "UPDATE certification SET cert_date = :cert_date where animal_id = :animal_id and user_id = :user_id";
                MapSqlParameterSource source3 = new MapSqlParameterSource().addValue("animal_id", animalId);
                source3.addValue("user_id", 999 );
                source3.addValue("cert_date", currentTime);
                jdbcTemplate.update(updateCertificate, source3);
                // Return HTML with image tag to display the uploaded image
                return "<img src='data:image/jpeg;base64," + base64Image + "' alt='" + fileName + "'>" +
                "<p>File recognized as " + data + ". and has been updated at" + currentTime+ 
                "<a href='/certificates'>Click here to insert a new certificate</a> <br></p>";
            }
            else{
                MapSqlParameterSource source2 = new MapSqlParameterSource().addValue("cert_image",fileBytes);
                source2.addValue("animal_id", animalId );
                source2.addValue("user_id", 999 );
                source2.addValue("cert_date", currentTime);


                String InsertCertificate = "INSERT INTO certification (cert_image, animal_id, user_id, cert_date) VALUES (:cert_image, :animal_id, :user_id, :cert_date)";
                jdbcTemplate.update(InsertCertificate, source2);

                // Return HTML with image tag to display the uploaded image
                return "<img src='data:image/jpeg;base64," + base64Image + "' alt='" + fileName + "'>" +
                "<p>File recognized as " + data + ". <a href='/certificates'>Click here to insert a new certificate</a> <br></p>";
            }
        }
    }

    @RequestMapping("/certificates/list")
    String certificatesList() {
        Context context = new Context();
        String GetCertificates= "SELECT a_name, cert_date, details, regions from certification JOIN animal on animal_id = a_id where user_id=999"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, new MapSqlParameterSource());
        
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
        context.setVariable("animalsNameRegistered", AnimalsName);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescription);
        context.setVariable("animalsRegionsRegistered", AnimalsRegions);
        context.setVariable("certificateDate", CertificatesDate);

        String html = templateEngine.process("personaltable", context);
        return html;
    }

    @RequestMapping(value="/certificates/list/filter", method = RequestMethod.POST)
    String certificatesFilteredList(@RequestParam("filter") String regione) {
        Context context = new Context();
        MapSqlParameterSource filterSearch = new MapSqlParameterSource().addValue("filter", regione);
        String GetCertificates= "SELECT a_name, cert_date, details, regions from certification JOIN animal on animal_id = a_id where user_id=999 AND regions ILIKE concat('%',:filter ,'%')"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, filterSearch);
        
        if(rows.isEmpty()){
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
        context.setVariable("animalsNameRegistered", AnimalsName);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescription);
        context.setVariable("animalsRegionsRegistered", AnimalsRegions);
        context.setVariable("certificateDate", CertificatesDate);

        String html = templateEngine.process("personaltablefiltered", context);
        return html;
    }

    @RequestMapping("/certificates/image")
    String ShowImage(@RequestParam("animal") String name) {
        //Shows the last uploaded image for the selected certificate
        String GetImage= "SELECT cert_image from certification JOIN animal on animal_id=a_id where a_name=:name";
        MapSqlParameterSource source7 = new MapSqlParameterSource().addValue("name", name);
        byte[] imageBytes = jdbcTemplate.queryForObject(GetImage, source7, byte[].class);
        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        return "<img src='data:image/jpeg;base64," + base64Image + "' alt='" + name + "'>"+
        "<br> <a href='/certificates'> Go back to upload certificates </a> <br>"+
        "<a href='../certificates/list'> Go back to your certificates</a>";
    }
    
    @RequestMapping("/certificates/animals")
    String AnimalsList() {
        Context context = new Context();
        String GetCertificates= "SELECT a_id, a_name, details, regions from animal"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, new MapSqlParameterSource());

        String Getids= "SELECT animal_id from certification where user_id=999"; //user=999 to be replaced by current_user
        List<Map<String, Object>> ids = jdbcTemplate.queryForList(Getids, new MapSqlParameterSource());

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

        context.setVariable("animalsNameRegistered", AnimalsNameRegistered);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescriptionRegistered);
        context.setVariable("animalsRegionsRegistered", AnimalsRegionsRegistered);
        context.setVariable("animalsName", AnimalsName);


        String html = templateEngine.process("table", context);
        return html;
    }

    @RequestMapping("/certificates/animals/increasingOrder")
    String AnimalsListIncOrder() {
        Context context = new Context();
        String GetCertificates= "SELECT a_id, a_name, details, regions from animal order by a_name"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, new MapSqlParameterSource());

        String Getids= "SELECT animal_id from certification where user_id=999"; //user=999 to be replaced by current_user
        List<Map<String, Object>> ids = jdbcTemplate.queryForList(Getids, new MapSqlParameterSource());

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

        context.setVariable("animalsNameRegistered", AnimalsNameRegistered);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescriptionRegistered);
        context.setVariable("animalsRegionsRegistered", AnimalsRegionsRegistered);
        context.setVariable("animalsName", AnimalsName);


        String html = templateEngine.process("tableinc", context);
        return html;
    }

    @RequestMapping("/certificates/animals/decreasingOrder")
    String AnimalsListDecOrder() {
        Context context = new Context();
        String GetCertificates= "SELECT a_id, a_name, details, regions from animal order by a_name DESC"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, new MapSqlParameterSource());

        String Getids= "SELECT animal_id from certification where user_id=999"; //user=999 to be replaced by current_user
        List<Map<String, Object>> ids = jdbcTemplate.queryForList(Getids, new MapSqlParameterSource());

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

        context.setVariable("animalsNameRegistered", AnimalsNameRegistered);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescriptionRegistered);
        context.setVariable("animalsRegionsRegistered", AnimalsRegionsRegistered);
        context.setVariable("animalsName", AnimalsName);


        String html = templateEngine.process("tableinv", context);
        return html;
    }

    @RequestMapping(value="/certificates/animals/search", method = RequestMethod.POST)
    String AnimalSearch(@RequestParam("searchAnimal") String animalsearch) {
        Context context = new Context();
        MapSqlParameterSource sourceSearch = new MapSqlParameterSource().addValue("animalName", animalsearch);
        String GetCertificates= "SELECT a_id, a_name, details, regions from animal where a_name ILIKE concat('%',:animalName ,'%')"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, sourceSearch);

        if(rows.isEmpty()){
            context.setVariable("error", "No such animal found");
            String html = templateEngine.process("tableError", context);
            return html;
        }

        String Getids= "SELECT animal_id from certification where user_id=999"; //user=999 to be replaced by current_user
        List<Map<String, Object>> ids = jdbcTemplate.queryForList(Getids, new MapSqlParameterSource());

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

        context.setVariable("animalsNameRegistered", AnimalsNameRegistered);
        context.setVariable("animalsDescriptionRegistered", AnimalsDescriptionRegistered);
        context.setVariable("animalsRegionsRegistered", AnimalsRegionsRegistered);
        context.setVariable("animalsName", AnimalsName);


        String html = templateEngine.process("tableSearch", context);
        return html;
    }
}

