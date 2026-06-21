package ru.practicum.shareit.booking.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class BookingDto {
    Long id;

    LocalDateTime start;

    LocalDateTime end;

    ItemDto item;

    UserDto booker;

    BookingStatus status;
}
