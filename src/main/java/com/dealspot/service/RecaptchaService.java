package com.dealspot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${recaptcha.secret-key}")
    private String secretKey;

    @Value("${recaptcha.threshold}")
    private double threshold;

    @Value("${recaptcha.enabled}")
    private boolean enabled;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Validates reCAPTCHA token. Returns true if validation passes.
     * When recaptcha is disabled (dev mode), always returns true.
     */
    public boolean verify(String recaptchaToken) {
        if (!enabled) {
            return true; // Skip in dev
        }

        if (recaptchaToken == null || recaptchaToken.isBlank()) {
            return false;
        }

        try {
            String url = "https://www.google.com/recaptcha/api/siteverify"
                    + "?secret=" + secretKey
                    + "&response=" + recaptchaToken;

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, null, Map.class);

            if (response == null) return false;

            Boolean success = (Boolean) response.get("success");
            Double score = response.get("score") != null ? ((Number) response.get("score")).doubleValue() : 0.0;

            return Boolean.TRUE.equals(success) && score >= threshold;
        } catch (Exception e) {
            return false;
        }
    }
}
