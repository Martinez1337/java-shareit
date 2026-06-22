package ru.practicum.shareit.item.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.CommentMapper;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemMapper;
import ru.practicum.shareit.item.dto.ItemOwnerDto;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.CommentRepository;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemServiceImpl implements ItemService {
    private final ItemRepository itemRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final ItemRequestRepository itemRequestRepository;
    private final ItemMapper itemMapper;
    private final CommentMapper commentMapper;

    @Override
    @Transactional
    public ItemDto create(Long userId, ItemDto itemDto) {
        User owner = getUser(userId);
        Item item = itemMapper.map(itemDto);
        item.setOwner(owner);
        if (itemDto.getRequestId() != null) {
            item.setRequest(getRequest(itemDto.getRequestId()));
        }
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
        Item item = getItem(itemId);
        ItemDto itemDto = itemMapper.mapToDto(item);
        itemDto.setComments(mapToCommentDtos(commentRepository.findAllByItemId(itemId)));
        if (item.getOwner().getId().equals(userId)) {
            List<Booking> bookings = getBookingsByItemId(List.of(itemId)).getOrDefault(itemId, List.of());
            setBookingDates(itemDto, bookings, now());
        }
        return itemDto;
    }

    @Override
    public List<ItemOwnerDto> getAllByOwnerId(Long userId) {
        getUser(userId);
        List<Item> items = itemRepository.findAllByOwner_Id(userId);
        List<Long> itemIds = items.stream()
                .map(Item::getId)
                .toList();
        Map<Long, List<Booking>> bookingsByItemId = getBookingsByItemId(itemIds);
        Map<Long, List<Comment>> commentsByItemId = getCommentsByItemId(itemIds);
        LocalDateTime now = now();

        return items.stream()
                .map(item -> mapToOwnerDto(item, bookingsByItemId, commentsByItemId, now))
                .toList();
    }

    @Override
    public List<ItemDto> search(String text) {
        return itemRepository.searchAvailableItems(text)
                .stream()
                .map(itemMapper::mapToDto)
                .toList();
    }

    @Override
    @Transactional
    public CommentDto addComment(Long userId, Long itemId, CommentCreateDto commentCreateDto) {
        User author = getUser(userId);
        Item item = getItem(itemId);
        LocalDateTime now = now();

        boolean userBookedItem = bookingRepository.existsByItem_IdAndBooker_IdAndStatusAndEndBefore(
                itemId,
                userId,
                BookingStatus.APPROVED,
                now
        );
        if (!userBookedItem) {
            throw new BadRequestException("User has not completed booking for item: " + itemId);
        }

        Comment comment = commentMapper.map(commentCreateDto)
                .setItem(item)
                .setAuthor(author)
                .setCreated(now);

        return commentMapper.mapToDto(commentRepository.save(comment));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private Item getItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
    }

    private ItemRequest getRequest(Long requestId) {
        return itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Item request not found: " + requestId));
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

    private Map<Long, List<Comment>> getCommentsByItemId(List<Long> itemIds) {
        if (itemIds.isEmpty()) {
            return Map.of();
        }

        return commentRepository.findAllByItemIds(itemIds)
                .stream()
                .collect(Collectors.groupingBy(comment -> comment.getItem().getId()));
    }

    private ItemOwnerDto mapToOwnerDto(
            Item item,
            Map<Long, List<Booking>> bookingsByItemId,
            Map<Long, List<Comment>> commentsByItemId,
            LocalDateTime now
    ) {
        ItemOwnerDto itemOwnerDto = itemMapper.mapToOwnerDto(item);
        List<Booking> bookings = bookingsByItemId.getOrDefault(item.getId(), List.of());
        itemOwnerDto.setComments(mapToCommentDtos(commentsByItemId.getOrDefault(item.getId(), List.of())));
        setBookingDates(itemOwnerDto, bookings, now);

        return itemOwnerDto;
    }

    private void setBookingDates(ItemDto itemDto, List<Booking> bookings, LocalDateTime now) {
        findLastBookingEnd(bookings, now).ifPresent(itemDto::setLastBooking);
        findNextBookingStart(bookings, now).ifPresent(itemDto::setNextBooking);
    }

    private void setBookingDates(ItemOwnerDto itemOwnerDto, List<Booking> bookings, LocalDateTime now) {
        findLastBookingEnd(bookings, now).ifPresent(itemOwnerDto::setLastBooking);
        findNextBookingStart(bookings, now).ifPresent(itemOwnerDto::setNextBooking);
    }

    private Optional<LocalDateTime> findLastBookingEnd(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(booking -> booking.getEnd().isBefore(now))
                .max(Comparator.comparing(Booking::getEnd))
                .map(Booking::getEnd);
    }

    private Optional<LocalDateTime> findNextBookingStart(List<Booking> bookings, LocalDateTime now) {
        return bookings.stream()
                .filter(booking -> booking.getStart().isAfter(now))
                .min(Comparator.comparing(Booking::getStart))
                .map(Booking::getStart);
    }

    private List<CommentDto> mapToCommentDtos(List<Comment> comments) {
        return comments.stream()
                .map(commentMapper::mapToDto)
                .toList();
    }

    private LocalDateTime now() {
        return LocalDateTime.now();
    }
}
