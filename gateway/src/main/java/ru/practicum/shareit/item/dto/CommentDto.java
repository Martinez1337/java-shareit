package ru.practicum.shareit.item.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class CommentDto {
    Long id;

    String text;

    String authorName;

    LocalDateTime created;
}
