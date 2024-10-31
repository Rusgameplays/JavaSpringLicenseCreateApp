package ru.mtuci.demo.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name="licenses")
public class License {
    @Id
    @Column(name="key")
    private String key;
    @Column(name="activationDate")
    private Date activationDate;
    @Column(name="expirationDate")
    private Date expirationDate;
    @Column(name="blocked")
    private Boolean blocked;
    @Column(name="deviceId")
    private String deviceId;


    @ManyToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    @JsonBackReference("back")
    private User user;

}
