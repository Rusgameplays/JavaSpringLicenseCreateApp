package ru.mtuci.demo.controller.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class LicenseHistoryResponse {
    private Long id;
    private Long licenseId;
    private Long userId;
    private String status;
    private Date changeDate;
    private String description;

    public LicenseHistoryResponse(Long licenseId, Long userId, String status, Date changeDate, String description) {
        this.licenseId = licenseId;
        this.userId = userId;
        this.status = status;
        this.changeDate = changeDate;
        this.description = description;
    }
}
