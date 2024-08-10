package it.uniroma1.animaldex;

import java.io.IOException;
import java.sql.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.thymeleaf.TemplateEngine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import java.util.ArrayList;
import java.util.Base64;
import java.time.LocalDateTime;
import org.json.JSONObject;
import java.util.List;
import java.util.Map;

@RestController
public class MapControler {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private TemplateEngine templateEngine;

    @RequestMapping("/{user_id}/map")
    public ResponseEntity<String> certificates(@PathVariable int user_id) {
        String base64North = getImageBase64(user_id, "North");
            String base64Center = getImageBase64(user_id, "Central");
            String base64South = getImageBase64(user_id, "South");
            String base64Islands = getImageBase64(user_id, "Islands");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode responseJson = mapper.createObjectNode();
        
        responseJson.put("north", base64North);
        responseJson.put("center", base64Center);
        responseJson.put("south", base64South);
        responseJson.put("islands", base64Islands);
        try {
            String jsonResponse = mapper.writeValueAsString(responseJson);
            return ResponseEntity.ok(jsonResponse);
        } catch (Exception e) {
            // Handle exception
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error occurred while processing the request.");
        }
    }  

    private String getImageBase64(int userId, String region) {
        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            params.addValue("user_id", userId);
            params.addValue("region", "%" + region + "%");

            String query = "SELECT cert_image FROM certification " +
                           "JOIN animal ON a_id = animal_id " +
                           "WHERE user_id = :user_id AND (regions LIKE :region OR regions LIKE '%All%') " +
                           "ORDER BY cert_date DESC LIMIT 1";

            List<byte[]> resultList = jdbcTemplate.query(query, params, (rs, rowNum) -> rs.getBytes("cert_image"));

            if (resultList.isEmpty()) {
                return ""; // Nessun risultato trovato, ritorna stringa vuota
            } else {
                return Base64.getEncoder().encodeToString(resultList.get(0));
            }

        } catch (Exception e) {
            // Log dell'errore specifico della query
            e.printStackTrace();
            return "";
        }
    }
}

