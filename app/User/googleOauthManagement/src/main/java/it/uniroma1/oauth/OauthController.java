package it.uniroma1.oauth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
public class OauthController {

    @GetMapping("/oauth/google")
    public RedirectView redirectToGoogle() {
        String clientId = "894483498590-vce84rh69ulm5hvbckqs507d6biu9q3r.apps.googleusercontent.com";
        String redirectUri = "http://localhost:8080/callback";
        String url = "https://accounts.google.com/o/oauth2/auth";

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri)
                .queryParam("scope", "email profile");

        return new RedirectView(builder.toUriString());
    }

    @RequestMapping("/callback")
    @ResponseBody
    public String callback(String code) {
        // Exchange code for access token
        // Make a request to Google API to get user info
        // Check if the user already exists in your system, if not, create a new user
        // Send notification to the server about the new user (for example, via REST API)
        return "Successfully logged in with Google!";
    }
}
