package ru.practicum.shareit.booking.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.practicum.shareit.validation.ValidationGroups.Create;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class BookingCreateDto {
    @NotNull(groups = Create.class)
    LocalDateTime start;

    @NotNull(groups = Create.class)
    LocalDateTime end;

    @NotNull(groups = Create.class)
    Long itemId;
}
