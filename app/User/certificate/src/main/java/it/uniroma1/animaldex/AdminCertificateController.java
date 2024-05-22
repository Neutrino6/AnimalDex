package it.uniroma1.animaldex;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@RestController
public class AdminCertificateController {

    @Autowired
    NamedParameterJdbcTemplate jdbcTemplate;
    @Autowired
    private TemplateEngine templateEngine;

    @RequestMapping("/{user_id}/events")
    public String events(@PathVariable int user_id,@CookieValue(value = "authCookie", defaultValue = "") String authCookieValue,@RequestParam(value = "msg", required = false) String msg) {
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

        String getAnimalNames="select a_name from animal";
        List<String> names = jdbcTemplate.queryForList(getAnimalNames, new MapSqlParameterSource(), String.class);

        Context context = new Context();
        if(msg != null){
            if(msg.equals("OK")){
                context.setVariable("msg", "New event created successfully");
            }
            else if(msg.equals("KO")){
                context.setVariable("msg", "There exists an event in progress for this animal");
            }
        }
        context.setVariable("names", names);
        context.setVariable("userId", ""+user_id+"");
        context.setVariable("link1", "/"+user_id+"/certificates");
        String html = templateEngine.process("events", context);
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
