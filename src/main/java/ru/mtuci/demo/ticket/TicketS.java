package ru.mtuci.demo.ticket;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@RequiredArgsConstructor
@Component
public class TicketS {
    private static TicketS instance;
    @PostConstruct
    private void init(){
        instance=this;
    }
    public static TicketS getInstance(){
        return instance;
    }
    private String HMAC_SHA256 = "HmacSHA256";
    @Value("${digitalKey}")
    private String SECRET_KEY;

    private ObjectMapper objectMapper = new ObjectMapper();

    public String serialize() {
        try {
            return objectMapper.writeValueAsString(this);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка сериализации объекта Ticket", e);
        }
    }

    public String generateDigitalSignature() {
        try {
            String data = serialize();
            SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(), HMAC_SHA256);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKey);

            byte[] rawHmac = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании цифровой подписи", e);
        }
    }
}
