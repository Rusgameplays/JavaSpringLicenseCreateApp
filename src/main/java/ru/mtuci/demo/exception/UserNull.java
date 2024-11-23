package ru.mtuci.demo.exception;

public class UserNull extends RuntimeException {
    public UserNull() {
        super("User not found");
    }

}
