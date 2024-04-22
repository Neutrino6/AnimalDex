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
        String GetCertificates= "SELECT a_name, cert_date, details, regions from certification JOIN animal on animal_id = a_id where user_id=999"; //user=999 to be replaced by current_user
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(GetCertificates, new MapSqlParameterSource());
        
        // Generating a string to add every entry in the database to appear in the table
        StringBuilder tableHtml = new StringBuilder();
        tableHtml.append("<h1> Uploaded certificates</h1>");
        tableHtml.append("<table border=\"1\">");
        tableHtml.append("<thead>");
        tableHtml.append("<tr>");
        tableHtml.append("<th>Animal name</th>");
        tableHtml.append("<th>Certificate Date</th>");
        tableHtml.append("<th>Details</th>");
        tableHtml.append("<th>Regions</th>");
        tableHtml.append("</tr>");
        tableHtml.append("</thead>");
        tableHtml.append("<tbody>");

        for (Map<String, Object> row : rows) {
            String name = (String) row.get("a_name");
            Date certDate = (Date) row.get("cert_date");
            String details = (String) row.get("details");
            String region = (String) row.get("regions");
    
            tableHtml.append("<tr>");
            tableHtml.append("<td><a href='../certificates/image?animal=").append(name).append("''>").append(name).append("</td>");
            tableHtml.append("<td>").append(certDate).append("</td>");
            tableHtml.append("<td>").append(details).append("</td>");
            tableHtml.append("<td>").append(region).append("</td>");
            tableHtml.append("</tr>");
        }
    
        tableHtml.append("</tbody>");
        tableHtml.append("</table>");
        tableHtml.append("<br> <a href='/certificates'> Go back </a>");
    
        return tableHtml.toString();
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

}

