package com.complipilot.backend.billing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.stripe")
public class StripeProperties {

    private String apiKey;
    private String webhookSecret;
    private String proPriceId;
    private String businessPriceId;
    private String enterprisePriceId;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getWebhookSecret() {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret) {
        this.webhookSecret = webhookSecret;
    }

    public String getProPriceId() {
        return proPriceId;
    }

    public void setProPriceId(String proPriceId) {
        this.proPriceId = proPriceId;
    }

    public String getBusinessPriceId() {
        return businessPriceId;
    }

    public void setBusinessPriceId(String businessPriceId) {
        this.businessPriceId = businessPriceId;
    }

    public String getEnterprisePriceId() {
        return enterprisePriceId;
    }

    public void setEnterprisePriceId(String enterprisePriceId) {
        this.enterprisePriceId = enterprisePriceId;
    }
}
