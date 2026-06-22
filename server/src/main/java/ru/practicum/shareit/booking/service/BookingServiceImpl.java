package ru.practicum.shareit.booking.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.dto.BookingMapper;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingState;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookingRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingDto create(Long userId, BookingCreateDto bookingCreateDto) {
        User booker = getUser(userId);

        Item item = getItem(bookingCreateDto.getItemId());
        if (!Boolean.TRUE.equals(item.getAvailable())) {
            throw new BadRequestException("Item is not available for booking: " + item.getId());
        }
        if (item.getOwner().getId().equals(userId)) {
            throw new NotFoundException("Owner cannot book own item: " + item.getId());
        }

        Booking booking = bookingMapper.map(bookingCreateDto)
                .setItem(item)
                .setBooker(booker)
                .setStatus(BookingStatus.WAITING);

        return bookingMapper.mapToDto(bookingRepository.save(booking));
    }

    @Override
    @Transactional
    public BookingDto updateApproval(Long userId, Long bookingId, boolean approved) {
        Booking booking = getBooking(bookingId);

        if (!booking.getItem().getOwner().getId().equals(userId)) {
            throw new BadRequestException("Booking not found for owner: " + userId);
        }
        if (booking.getStatus() != BookingStatus.WAITING) {
            throw new BadRequestException("Booking approval can be changed only from WAITING status");
        }

        booking.setStatus(approved ? BookingStatus.APPROVED : BookingStatus.REJECTED);
        return bookingMapper.mapToDto(bookingRepository.save(booking));
    }

    @Override
    public BookingDto getById(Long userId, Long bookingId) {
        getUser(userId);
        Booking booking = getBooking(bookingId);

        if (!booking.getBooker().getId().equals(userId)
                && !booking.getItem().getOwner().getId().equals(userId)) {
            throw new NotFoundException("Booking not found for user: " + userId);
        }

        return bookingMapper.mapToDto(booking);
    }

    @Override
    public List<BookingDto> getAllByUserId(Long userId, String state) {
        getUser(userId);
        BookingState bookingState = parseState(state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (bookingState) {
            case ALL -> bookingRepository.findAllByBooker_IdOrderByStartDesc(userId);
            case CURRENT -> bookingRepository.findCurrentByBookerId(userId, now);
            case PAST -> bookingRepository.findAllByBooker_IdAndEndBeforeOrderByStartDesc(userId, now);
            case FUTURE -> bookingRepository.findAllByBooker_IdAndStartAfterOrderByStartDesc(userId, now);
            case WAITING -> bookingRepository.findAllByBooker_IdAndStatusOrderByStartDesc(
                    userId,
                    BookingStatus.WAITING
            );
            case REJECTED -> bookingRepository.findAllByBooker_IdAndStatusOrderByStartDesc(
                    userId,
                    BookingStatus.REJECTED
            );
        };

        return mapToDtos(bookings);
    }

    @Override
    public List<BookingDto> getAllByOwnerId(Long userId, String state) {
        getUser(userId);
        BookingState bookingState = parseState(state);
        LocalDateTime now = LocalDateTime.now();

        List<Booking> bookings = switch (bookingState) {
            case ALL -> bookingRepository.findAllByOwnerId(userId);
            case CURRENT -> bookingRepository.findCurrentByOwnerId(userId, now);
            case PAST -> bookingRepository.findPastByOwnerId(userId, now);
            case FUTURE -> bookingRepository.findFutureByOwnerId(userId, now);
            case WAITING -> bookingRepository.findAllByOwnerIdAndStatus(userId, BookingStatus.WAITING);
            case REJECTED -> bookingRepository.findAllByOwnerIdAndStatus(userId, BookingStatus.REJECTED);
        };

        return mapToDtos(bookings);
    }

    private List<BookingDto> mapToDtos(List<Booking> bookings) {
        return bookings.stream()
                .map(bookingMapper::mapToDto)
                .toList();
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private Booking getBooking(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + bookingId));
    }

    private Item getItem(Long itemId) {
        return itemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item not found: " + itemId));
    }

    private BookingState parseState(String state) {
        return BookingState.valueOf(state.trim().toUpperCase(Locale.ROOT));
    }
}
