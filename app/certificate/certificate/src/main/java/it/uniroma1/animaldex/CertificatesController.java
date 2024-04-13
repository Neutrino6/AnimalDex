package it.uniroma1.animaldex;

import lombok.Data;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import java.util.Base64;


@RestController
public class CertificatesController {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;

    @RequestMapping("/certificates")
    String certificates() {
        return 
            "<form action=\"localhost:8080/certificates/manage\" method=\"post\" enctype=\"multipart/form-data\">\r\n" + //
            "    <label for=\"fileInput\">Upload a certificate and get points:</label><br>\r\n" + //
            "    <input type=\"file\" id=\"fileInput\" name=\"fileInput\" accept=\"image/*\" required><br><br>\r\n" + //
            "    <input type=\"submit\" value=\"Upload certificate\">\r\n" + //
            "</form>\r\n";
    }

    @RequestMapping(value="/certificates/manage", method = RequestMethod.POST)
    public String manage(@RequestParam("fileInput") MultipartFile fileInput) {
        // Post managed to download image and then send it to the recognition page
        if (fileInput.isEmpty()){
            return "error";
        }

        try{
            //preparation of the request to the recognition page
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            //image encoded in base 64 so to send it easily
            byte[] bytes = fileInput.getBytes();
            String base64_img= Base64.getEncoder().encodeToString(bytes);

            
            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            
            map.add("fileInput", base64_img);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
            String pythonUrl = "http://localhost:5000/predict";
            ResponseEntity<String> response = restTemplate.postForEntity(pythonUrl, request, String.class);

            // return value of the request as a string
            String response2=response.getBody();

            return response2; // routing to the page returned
        } catch (IOException e) {
            e.printStackTrace();
            return ""; 
        }
    }


    @RequestMapping(value = "/certificates/upload", method = RequestMethod.POST)
    public String upload(@RequestParam("data") String data) {
        // Check if file is empty
        if (data.isEmpty()) {
            return "Error: File is empty";
        }
        if(data.equals("UNRECOGNIZED")){
            return "File unrecognized. <a href='/certificates'>Click here to insert a new certificate</a>";
        }
        else{
            return "File recognized as "+data+". <a href='/certificates'>Click here to insert a new certificate</a>";
        }
    }
}

