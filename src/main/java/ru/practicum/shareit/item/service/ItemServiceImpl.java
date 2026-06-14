package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemMapper;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemMapper itemMapper;

    @Override
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = getUser(userId);
        Item item = itemMapper.map(itemDto);
        item.setOwner(owner);
        return itemMapper.mapToDto(itemRepository.create(item));
    }

    @Override
    public ItemDto update(Long userId, Long itemId, ItemDto itemDto) {
        getUser(userId);
        Item item = getItem(itemId);

        if (!item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Item not found for owner: " + userId);
        }
        if (itemDto.getName() != null) {
            item.setName(itemDto.getName());
        }
        if (itemDto.getDescription() != null) {
            item.setDescription(itemDto.getDescription());
        }
        if (itemDto.getAvailable() != null) {
            item.setAvailable(itemDto.getAvailable());
        }

        return itemMapper.mapToDto(itemRepository.update(item));
    }

    @Override
    public ItemDto getById(Long userId, Long itemId) {
        getUser(userId);
        return itemMapper.mapToDto(getItem(itemId));
    }

    @Override
    public List<ItemDto> getByOwner(Long userId) {
        getUser(userId);
        return itemRepository.findAllByOwnerId(userId)
                .stream()
                .map(itemMapper::mapToDto)
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        return itemRepository.search(text)
                .stream()
                .map(itemMapper::mapToDto)
                .toList();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private Item getItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
    }
}
