package it.uniroma1.animaldex;

import lombok.Data;

import java.io.IOException;

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

import java.util.Base64;

@RestController
public class CertificatesController {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @RequestMapping("/certificates")
    String certificates() {
        return 
            "<form action=\"http://localhost:5000/predict\" method=\"post\" enctype=\"multipart/form-data\">\r\n" + //
            "    <label for=\"fileInput\">Upload a certificate and get points:</label><br>\r\n" + //
            "    <input type=\"file\" id=\"fileInput\" name=\"fileInput\" accept=\"image/*\" required><br><br>\r\n" + //
            "    <input type=\"submit\" value=\"Upload certificate\">\r\n" + //
            "</form>\r\n";
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
        try {
            byte[] fileBytes = fileInput.getBytes();
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
            
                // Add response validation logic here if needed
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
                e.printStackTrace(); // Print detailed stack trace for debugging
            }
            
            // Return HTML with image tag to display the uploaded image
            return "<img src='data:image/jpeg;base64," + base64Image + "' alt='" + fileName + "'>" +
                "<p>File recognized as " + data + ". <a href='/certificates'>Click here to insert a new certificate</a> <br></p>";
        }
    }

}

