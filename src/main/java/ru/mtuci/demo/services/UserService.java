package ru.mtuci.demo.services;

import ru.mtuci.demo.model.User;
import ru.mtuci.demo.exception.UserAlreadyCreate;

import java.util.List;
import java.util.UUID;

public interface UserService {
    List<User> getAll();
    User getById(Long id);
    User getByName(String name);

    void create(String email, String name, String password) throws UserAlreadyCreate;

    void createAdmin(String email, String name, String password) throws UserAlreadyCreate;

}
