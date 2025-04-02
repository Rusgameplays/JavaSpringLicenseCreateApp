package ru.mtuci.demo.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;
import java.time.LocalDateTime;


@Getter
@Setter
@Entity
@Table(name = "signatures")
public class Signature {

    @Id
    @GeneratedValue(generator = "UUID")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "threat_name", nullable = false)
    private String threatName;

    @Column(name = "first_bytes", length = 8, nullable = false)
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
    @Column(name = "digital_signature")
    private byte[] digitalSignature;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusSignature status;

}
