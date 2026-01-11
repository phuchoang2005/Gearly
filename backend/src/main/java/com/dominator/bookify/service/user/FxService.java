package com.dominator.bookify.service.user;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
public class FxService {
    private static final String FX_API_URL = "https://open.er-api.com/v6/latest/USD";
    private final RestTemplate restTemplate = new RestTemplate();
    private static final BigDecimal DEFAULT_RATE = new BigDecimal("23000");

    public BigDecimal getUsdToVndRate() {
        try {
            JsonNode root = restTemplate.getForObject(FX_API_URL, JsonNode.class);
            if (root != null) {
                JsonNode vndNode = root.path("rates").path("VND");
                if (!vndNode.isMissingNode()) {
                    String vndText = vndNode.asText();
                    if (vndText != null && !vndText.isEmpty()) {
                        return new BigDecimal(vndText);
                    }
                }
            }
        } catch (Exception ignored) { }
        return DEFAULT_RATE;
    }
}
