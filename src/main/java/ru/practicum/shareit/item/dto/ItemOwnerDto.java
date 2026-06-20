package ru.practicum.shareit.item.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ItemOwnerDto {
    Long id;

    String name;

    String description;

    Boolean available;

    Long ownerId;

    Long requestId;

    LocalDateTime lastBooking;

    LocalDateTime nextBooking;
}
