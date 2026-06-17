package com.c15tour.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MailjetEmailService {

    private static final Logger logger = LoggerFactory.getLogger(MailjetEmailService.class);
    private final RestClient restClient;
    private final String fromEmail;

    public MailjetEmailService(
            @Value("${mailjet.api-key}") String apiKey,
            @Value("${mailjet.secret-key}") String secretKey,
            @Value("${mailjet.from-email}") String fromEmail) {
        this.fromEmail = fromEmail;

        String basicAuth = "Basic " + Base64.getEncoder()
                .encodeToString((apiKey + ":" + secretKey).getBytes(StandardCharsets.UTF_8));

        this.restClient = RestClient.builder()
                .baseUrl("https://api.mailjet.com")
                .defaultHeader("Authorization", basicAuth)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public void sendPasswordResetEmail(String toEmail, String resetUrl) {
        try {
            Map<String, Object> request = new HashMap<>();

            Map<String, Object> message = new HashMap<>();

            Map<String, String> from = new HashMap<>();
            from.put("Email", fromEmail);
            from.put("Name", "C15Tour");
            message.put("From", from);

            Map<String, String> to = new HashMap<>();
            to.put("Email", toEmail);
            message.put("To", List.of(to));

            message.put("Subject", "Réinitialisation de votre mot de passe");
            message.put("HTMLPart",
                    "<p>Bonjour,</p>" +
                    "<p>Cliquez sur le lien ci-dessous pour réinitialiser votre mot de passe (valable 1 heure) :</p>" +
                    "<p><a href='" + resetUrl + "'>Réinitialiser mon mot de passe</a></p>" +
                    "<p>Si vous n'avez pas fait cette demande, ignorez cet email.</p>");
            message.put("TextPart",
                    "Pour réinitialiser votre mot de passe, accédez à ce lien (valable 1 heure): " + resetUrl);

            request.put("Messages", List.of(message));

            restClient.post()
                    .uri("/v3.1/send")
                    .body(request)
                    .retrieve()
                    .body(String.class);

            logger.info("Password reset email sent to {}", toEmail);
        } catch (RestClientException e) {
            logger.warn("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }
}
