package ru.mtuci.demo.ticket;

import java.util.Base64;
import java.util.Date;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import ru.mtuci.demo.model.Device;
import ru.mtuci.demo.model.License;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import java.util.Base64;
import java.util.Date;
import lombok.RequiredArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.NoArgsConstructor;
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

    public Ticket(License license, Device device) {
        this.serverDate = new Date();
        this.ticketLifetime = license.getLicenseType().getDefaultDuration() != null
                ? license.getLicenseType().getDefaultDuration().longValue() * 30 * 24 * 60 * 60 * 1000
                : 0L;
        this.activationDate = license.getActivationDate();
        this.expirationDate = license.getExpirationDate();
        this.userId = device.getUser() != null ? device.getUser().getId() : null;
        this.deviceId = device.getMac();
        this.licenseBlocked = license.getBlocked() != null ? license.getBlocked().toString() : "null";
        this.digitalSignature=TicketS.getInstance().generateDigitalSignature();
    }
}


