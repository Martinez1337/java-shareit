package ru.practicum.shareit.item.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.practicum.shareit.validation.ValidationGroups.Create;

@Data
@Accessors(chain = true)
public class CommentCreateDto {
    @NotBlank(groups = Create.class)
    String text;
}
