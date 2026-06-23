package ru.practicum.shareit.request.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ItemRequestDto {
    Long id;

    @NotBlank
    String description;

    LocalDateTime created;

    List<ItemRequestResponseDto> items;
}
