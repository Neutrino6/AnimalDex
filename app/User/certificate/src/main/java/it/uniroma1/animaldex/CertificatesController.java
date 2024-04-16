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
            return "Error: File is empty";
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
            return "Error processing file";
        }
        
        // If data is UNRECOGNIZED, handle the fileInput
        if (data.equals("UNRECOGNIZED")) {
            // Return a response message or perform further actions based on the file content
            return "File unrecognized. <a href='/certificates'>Click here to insert a new certificate</a>";
        } else {

            //Send request to centralServer for validation

            HttpClient client = HttpClients.createDefault();
        
            // Create RequestBody
            String jsonBody = "{\"animalName\":\""+data+"\"}";
            
            // Create HTTP request
            HttpPost request = new HttpPost("http://localhost:6039/newCertificate");
            request.setHeader("Content-Type", "application/json");
            request.setEntity(new StringEntity(jsonBody, "UTF-8"));
            
            // execute the request and manage the response
            try {
                HttpResponse response = client.execute(request);
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity);
                
                System.out.println("Status code: " + statusCode);
                System.out.println("Response body: " + responseBody);
            } catch (IOException e) {
                System.err.println("Error: " + e.getMessage());
            }

            // Return HTML with image tag to display the uploaded image
            return "<img src='data:image/jpeg;base64," + base64Image + "' alt='" + fileName + "'>" +
                "<p>File recognized as " + data + ". <a href='/certificates'>Click here to insert a new certificate</a> <br></p>";
        }
    }

}

