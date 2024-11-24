package ru.mtuci.demo.ticket;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.License;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@NoArgsConstructor
@Data
public class Ticket {

    private Date serverDate;
    private Long ticketLifetime;
    private Date activationDate;
    private Date expirationDate;
    private Long userId;
    private String deviceId;
    private String licenseBlocked;
    private String digitalSignature;
    @JsonIgnore
    private License license;
    @JsonIgnore
    private Device device;

    public class DigitalSignatureUtil {

        private static final String HMAC_SHA256 = "HmacSHA256";


        private static final String SECRET_KEY = "key"; //Скрою позже

        public static String generateSignature(String data) {
            try {
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

}
