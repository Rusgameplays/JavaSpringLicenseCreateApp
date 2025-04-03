package ru.mtuci.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "audit")
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id", updatable = false, nullable = false)
    private Long auditId;

    @Column(name = "signature_id", nullable = false)
    private UUID signatureId;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "change_type", nullable = false)
    private String changeType;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Lob
    @Column(name = "fields_changed", nullable = false)
    private String fieldsChanged;
}

