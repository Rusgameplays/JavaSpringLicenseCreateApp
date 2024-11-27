package ru.mtuci.demo.services;

import ru.mtuci.demo.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    Product getProductById(Long id);
    Product addProduct(Product product);
    Product getById(Long id);
    void update(Product product);
    void deleteById(Long id);
}
