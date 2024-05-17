package it.uniroma1.animaldex;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Date;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RestController
public class ScoreBoardController {
    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private TemplateEngine templateEngine;
    @RequestMapping("/{user_id}/scoreboard")
    public String scoreboard(@PathVariable int user_id,@CookieValue(value = "authCookie", defaultValue = "") String authCookieValue) {
        int check=isValidAuthCookie(authCookieValue,user_id);
        JSONObject jsonObject;
        String deadline;
        if(check==0){
            Context context = new Context();
            context.setVariable("error", "You are not authorized to perform this action.");
            context.setVariable("redirectUrl","http://localhost:3000/LoginUser.html");
            String html = templateEngine.process("errorPage", context);
            return html;
        }

        HttpPost request = new HttpPost("http://host.docker.internal:6039/getScoreBoard");  //communication between different containers
        
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);

        
            System.out.println("Status code: " + statusCode);
            System.out.println("Response body: " + responseBody);

            jsonObject = new JSONObject(responseBody);

        } catch (IOException e) {
            String error="Error: " + e.getMessage();
            System.err.println(error);
            e.printStackTrace(); // Print detailed stack trace for debugging
            Context context = new Context();
            context.setVariable("error", error);
            context.setVariable("redirectUrl","http://localhost:3000/LoginUser.html");
            String html = templateEngine.process("errorPage", context);
            return html;
        }

        request = new HttpPost("http://host.docker.internal:6040/getDeadline"); 
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            CloseableHttpResponse response = client.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);

        
            System.out.println("Status code: " + statusCode);
            System.out.println("Response body: " + responseBody);

            deadline = responseBody;

        } catch (IOException e) {
            String error="Error: " + e.getMessage();
            System.err.println(error);
            e.printStackTrace(); // Print detailed stack trace for debugging
            Context context = new Context();
            context.setVariable("error", error);
            context.setVariable("redirectUrl","http://localhost:3000/LoginUser.html");
            String html = templateEngine.process("errorPage", context);
            return html;
        }
        
        Context context=new Context();

        JSONArray users = jsonObject.getJSONArray("users");
        context.setVariable("users", users);
        context.setVariable("deadline", "The new winners will be announced on "+deadline);
        context.setVariable("link1", "/"+user_id+"/certificates");
        String html = templateEngine.process("scoreboard", context);
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
}
