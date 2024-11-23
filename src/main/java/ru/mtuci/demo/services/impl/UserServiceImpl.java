package ru.mtuci.demo.services.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.mtuci.demo.exception.UserAlreadyCreate;
import ru.mtuci.demo.model.ApplicationRole;
import ru.mtuci.demo.model.User;
import ru.mtuci.demo.repo.UserRepository;
import ru.mtuci.demo.services.UserService;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

@RequiredArgsConstructor
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public List<User> getAll() {
        return userRepository.findAll();
    }


    @Override
    public User getById(Long id) {
        return userRepository.findById(id).orElse(new User());
    }

    @Override
    public User getByName(String name)  {
        return userRepository.findByName(name).orElse(new User());
    }

    @Override
    public void create(String email, String name, String password) throws UserAlreadyCreate {
        if (userRepository.findByEmail(email).isPresent()) throw new UserAlreadyCreate(email);
        var user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(ApplicationRole.USER);
        userRepository.save(user);
    }

    @Override
    public void createAdmin(String email, String name, String password) throws UserAlreadyCreate {
        if (userRepository.findByEmail(email).isPresent()) throw new UserAlreadyCreate(email);
        var user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(ApplicationRole.ADMIN);
        userRepository.save(user);
    }

}
