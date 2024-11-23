package ru.mtuci.demo.exception;

public class TypeofLicenseNull extends RuntimeException {
    public TypeofLicenseNull() {
        super("Type of product not found");
    }

}
