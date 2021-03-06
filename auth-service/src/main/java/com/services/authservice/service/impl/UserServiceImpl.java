package com.services.authservice.service.impl;

import com.services.authservice.exception.AttemptsLimitException;
import com.services.authservice.exception.LoginException;
import com.services.authservice.exception.RegistrationException;
import com.services.authservice.model.Attempt;
import com.services.authservice.model.User;
import com.services.authservice.repository.AttemptRepository;
import com.services.authservice.repository.UserRepository;
import com.services.authservice.service.UserService;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
@Validated
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final AttemptRepository attemptRepository;

    @Value("${max-tries}")
    private int maxAttempts;

    @Value("${ban-time}")
    private Long expireTime;

    public UserServiceImpl(UserRepository userRepository, AttemptRepository attemptRepository) {
        this.userRepository = userRepository;
        this.attemptRepository = attemptRepository;
    }

    @Override
    public void register(String username, String userSecret) {

        if (userRepository.findByUsername(username).isPresent()) {
            throw new RegistrationException("User with name " + username + " already registered");
        }

        String hash = BCrypt.hashpw(userSecret, BCrypt.gensalt());
        User user = new User();
        user.setUsername(username);
        user.setHash(hash);
        userRepository.save(user);
    }

    @Override
    public void checkCredentials(String username, String userSecret) {

        int counter = attemptRepository.countFailAttempts(LocalDateTime.now().minus(expireTime, ChronoUnit.SECONDS), username);
        if (counter >= maxAttempts) {
            throw new AttemptsLimitException("Too many attempts for " + expireTime / 60 + " minutes. Come back later!");
        }

        Optional<User> optionalUserEntity = userRepository.findByUsername(username);

        if (optionalUserEntity.isEmpty()) {
            throw new LoginException("User with name " + username + " not found");
        }

        User user = optionalUserEntity.get();

        if (!BCrypt.checkpw(userSecret, user.getHash())) {
            Attempt attempt = new Attempt();
            attempt.setUsername(username);
            LocalDateTime creationDateTime = LocalDateTime.now();
            attempt.setCreationDateTime(creationDateTime);
            attemptRepository.save(attempt);

            throw new LoginException("Password is incorrect");
        } else {
            attemptRepository.deleteAttemptsByUsername(username);
        }
    }

    @Override
    public void checkUsername(String username) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RegistrationException("User with name " + username + " already registered");
        }
    }
}
