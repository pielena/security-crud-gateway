package com.services.animalservice.service.impl;

import com.services.animalservice.exception.AnimalServiceException;
import com.services.animalservice.model.Animal;
import com.services.animalservice.model.User;
import com.services.animalservice.repository.AnimalRepository;
import com.services.animalservice.service.AnimalService;
import com.services.animalservice.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnimalServiceImpl implements AnimalService {

    private final AnimalRepository animalRepository;
    private final UserService userService;

    public AnimalServiceImpl(AnimalRepository animalRepository, UserService userService) {
        this.animalRepository = animalRepository;
        this.userService = userService;
    }

    @Override
    public Animal save(Animal animal, String username) {
        if (!animalRepository.existsAnimalByName(animal.getName())) {
            User user = userService.getUserByUsername(username);
            animal.setUser(user);
            return animalRepository.save(animal);
        } else {
            throw new AnimalServiceException("Animal with name " + animal.getName() + " already exists");
        }
    }

    @Override
    public Animal update(Animal animal, String username) {
         Animal result = animalRepository.findById(animal.getId())
                 .orElseThrow(() -> new AnimalServiceException("Animal with id " + animal.getId() + " doesn't exist"));
         if (result.getUser().getUsername().equals(username)) {
             result.setBirthday(animal.getBirthday());
             result.setAnimalType(animal.getAnimalType());
             result.setAnimalSex(animal.getAnimalSex());
             result.setName(animal.getName());
             animalRepository.save(result);
         }
         else throw new AnimalServiceException("It's not your animal, you can't change it");

        return result;
    }

    @Override
    public void delete(Long animalId, String username) {
        Animal animal = getById(animalId);
        if (!animal.getUser().getUsername().equals(username)) {
            throw new AnimalServiceException("It's not your animal, you can't delete it");
        }
        User user = userService.getUserByUsername(username);
        Long userId = user.getId();
            animalRepository.deleteByUserIdAndAnimalId(animalId, userId);
    }

    @Override
    public Animal getById(Long animalId) {
        return animalRepository.findById(animalId)
                .orElseThrow(() -> new AnimalServiceException("Animal with id " + animalId + " doesn't exist"));
    }

    @Override
    public List<Animal> getAllByUsername(String username) {
        User user = userService.getUserByUsername(username);
        Long userId = user.getId();
        return animalRepository.findAnimalByUser(userId);
    }
}
