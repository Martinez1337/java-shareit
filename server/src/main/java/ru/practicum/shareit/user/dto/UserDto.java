package ru.practicum.shareit.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.experimental.Accessors;
import ru.practicum.shareit.validation.ValidationGroups.Create;
import ru.practicum.shareit.validation.ValidationGroups.Update;

@Data
@Accessors(chain = true)
public class UserDto {
    Long id;

    @NotBlank(groups = Create.class)
    @Pattern(regexp = ".*\\S.*", groups = Update.class)
    String name;

    @NotBlank(groups = Create.class)
    @Email(groups = {Create.class, Update.class})
    @Pattern(regexp = ".*\\S.*", groups = Update.class)
    String email;
}
