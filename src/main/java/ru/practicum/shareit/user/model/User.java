package ru.practicum.shareit.user.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class User {
    Long id;

    String name;

    String email;
}
