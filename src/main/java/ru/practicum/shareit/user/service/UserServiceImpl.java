package ru.practicum.shareit.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.ConflictException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.dto.UserMapper;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Override
    public UserDto create(UserDto userDto) {
        checkEmailIsFree(userDto.getEmail(), null);
        return userMapper.mapToDto(userRepository.create(userMapper.map(userDto)));
    }

    @Override
    public UserDto update(Long userId, UserDto userDto) {
        User user = getUser(userId);

        if (userDto.getName() != null) {
            user.setName(userDto.getName());
        }
        if (userDto.getEmail() != null) {
            checkEmailIsFree(userDto.getEmail(), userId);
            user.setEmail(userDto.getEmail());
        }

        return userMapper.mapToDto(userRepository.update(user));
    }

    @Override
    public UserDto getById(Long userId) {
        return userMapper.mapToDto(getUser(userId));
    }

    @Override
    public List<UserDto> getAll() {
        return userRepository.findAll()
                .stream()
                .map(userMapper::mapToDto)
                .toList();
    }

    @Override
    public void delete(Long userId) {
        userRepository.deleteById(userId);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private void checkEmailIsFree(String email, Long currentUserId) {
        userRepository.findByEmail(email)
                .filter(user -> !user.getId().equals(currentUserId))
                .ifPresent(user -> {
                    throw new ConflictException("Email already exists: " + email);
                });
    }
}
