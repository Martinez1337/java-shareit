package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemMapper;
import ru.practicum.shareit.item.dto.ItemOwnerDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ItemMapper itemMapper;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = getUser(userId);
        Item item = itemMapper.map(itemDto);
        item.setOwner(owner);
        return itemMapper.mapToDto(itemRepository.save(item));
    }

    @Override
    @Transactional
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

        return itemMapper.mapToDto(itemRepository.save(item));
    }

    @Override
    public ItemDto getById(Long userId, Long itemId) {
        getUser(userId);
        return itemMapper.mapToDto(getItem(itemId));
    }

    @Override
    public List<ItemOwnerDto> getAllByOwnerId(Long userId) {
        getUser(userId);
        List<Item> items = itemRepository.findAllByOwner_Id(userId);
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .toList();
        Map<Long, List<Booking>> bookingsByItemId = getBookingsByItemId(itemIds);
        LocalDateTime now = LocalDateTime.now();

        return items.stream()
                .map(item -> mapToOwnerDto(item, bookingsByItemId, now))
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        if (text == null || text.isBlank()) {
            return new ArrayList<>();
        }

        return itemRepository.searchAvailableItems(text)
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

    private Map<Long, List<Booking>> getBookingsByItemId(List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }

        return bookingRepository.findAllByItem_IdInAndStatusOrderByStartDesc(
                        itemIds,
                        BookingStatus.APPROVED
                )
                .stream()
                .collect(Collectors.groupingBy(booking -> booking.getItem().getId()));
    }

    private ItemOwnerDto mapToOwnerDto(
            Item item,
            Map<Long, List<Booking>> bookingsByItemId,
            LocalDateTime now
    ) {
        ItemOwnerDto itemOwnerDto = itemMapper.mapToOwnerDto(item);
        List<Booking> bookings = bookingsByItemId.getOrDefault(item.getId(), List.of());

        bookings.stream()
                .filter(booking -> booking.getEnd().isBefore(now))
                .max(Comparator.comparing(Booking::getEnd))
                .map(Booking::getEnd)
                .ifPresent(itemOwnerDto::setLastBooking);

        bookings.stream()
                .filter(booking -> booking.getStart().isAfter(now))
                .min(Comparator.comparing(Booking::getStart))
                .map(Booking::getStart)
                .ifPresent(itemOwnerDto::setNextBooking);

        return itemOwnerDto;
    }
}
