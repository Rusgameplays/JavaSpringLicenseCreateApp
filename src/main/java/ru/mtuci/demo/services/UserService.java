package ru.mtuci.demo.services;

import ru.mtuci.demo.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    List<User> getAll();
    void add(User user);

    Optional<User> getById(Long id);

    Optional<User> getByName(String name);
}
