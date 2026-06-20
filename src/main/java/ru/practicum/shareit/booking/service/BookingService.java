package ru.practicum.shareit.booking.service;

import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;

import java.util.List;

public interface BookingService {

    BookingDto create(Long userId, BookingCreateDto bookingCreateDto);

    BookingDto updateApproval(Long userId, Long bookingId, boolean approved);

    BookingDto getById(Long userId, Long bookingId);

    List<BookingDto> getAllByUserId(Long userId, String state);

    List<BookingDto> getAllByOwnerId(Long userId, String state);
}
