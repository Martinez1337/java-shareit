package ru.practicum.shareit.item.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ItemDto {
    Long id;

    String name;

    String description;

    Boolean available;

    Long ownerId;

    Long requestId;

    LocalDateTime lastBooking;

    LocalDateTime nextBooking;

    List<CommentDto> comments;
}
