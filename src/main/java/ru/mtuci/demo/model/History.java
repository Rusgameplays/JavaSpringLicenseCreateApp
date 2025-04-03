package ru.mtuci.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "history")
@Getter
@Setter
public class History {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id", updatable = false, nullable = false)
    private Long historyId;

    @Column(name = "signature_id", nullable = false)
    private UUID signatureId;

    @Column(name = "version_created_at", nullable = false)
    private LocalDateTime versionCreatedAt;

    @Column(name = "threat_name", nullable = false)
    private String threatName;

    @Column(name = "first_bytes", nullable = false)
    private byte[] firstBytes;

    @Column(name = "remainder_hash", nullable = false)
    private String remainderHash;

    @Column(name = "remainder_length", nullable = false)
    private int remainderLength;

    @Column(name = "file_type", nullable = false)
    private String fileType;

    @Column(name = "offset_start", nullable = false)
    private int offsetStart;

    @Column(name = "offset_end", nullable = false)
    private int offsetEnd;

    @Lob
    @Column(name = "digital_signature", nullable = false)
    private byte[] digitalSignature;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusSignature status;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
