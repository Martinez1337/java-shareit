package ru.practicum.shareit.request.dto;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class ItemRequestResponseDto {
    Long id;

    String name;

    Long ownerId;
}
