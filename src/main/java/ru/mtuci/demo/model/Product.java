package ru.mtuci.demo.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "products")
@Entity
public final class Product {
    @Id
    @GeneratedValue
    private Long id;

    @Column(unique = true)
    private String name;

    private Boolean blocked;
}