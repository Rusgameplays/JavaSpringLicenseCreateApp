package ru.mtuci.demo.exception;

public class ProductNull extends RuntimeException {
    public ProductNull() {
        super("Product not found");
    }

}
