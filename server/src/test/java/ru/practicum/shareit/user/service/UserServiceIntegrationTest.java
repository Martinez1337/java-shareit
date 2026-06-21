package ru.practicum.shareit.user.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class UserServiceIntegrationTest {
    @Autowired
    private UserService userService;

    @Test
    void createUpdateGetAllAndDeleteShouldPersistUserChanges() {
        UserDto created = userService.create(new UserDto()
                .setName("Ada")
                .setEmail("ada@example.com"));

        assertThat(created.getId()).isNotNull();
        assertThat(userService.getById(created.getId()).getEmail()).isEqualTo("ada@example.com");
        assertThat(userService.getAll()).extracting(UserDto::getName).contains("Ada");

        UserDto updated = userService.update(created.getId(), new UserDto()
                .setName("Ada Lovelace"));

        assertThat(updated.getName()).isEqualTo("Ada Lovelace");
        assertThat(updated.getEmail()).isEqualTo("ada@example.com");

        userService.delete(created.getId());

        assertThatThrownBy(() -> userService.getById(created.getId()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createShouldRejectDuplicateEmail() {
        userService.create(new UserDto()
                .setName("First")
                .setEmail("same@example.com"));

        assertThatThrownBy(() -> userService.create(new UserDto()
                .setName("Second")
                .setEmail("same@example.com")))
                .isInstanceOf(ConflictException.class);
    }
}
