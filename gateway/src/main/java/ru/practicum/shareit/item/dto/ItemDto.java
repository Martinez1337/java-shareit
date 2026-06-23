package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.practicum.shareit.validation.ValidationGroups.Update;
import ru.practicum.shareit.validation.ValidationGroups.Create;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ItemDto {
    Long id;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = ".*\\S.*", groups = Update.class)
    String name;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = ".*\\S.*", groups = Update.class)
    String description;

    @NotNull(groups = Create.class)
    Boolean available;

    Long ownerId;

    Long requestId;

    LocalDateTime lastBooking;

    LocalDateTime nextBooking;

    List<CommentDto> comments;
}
