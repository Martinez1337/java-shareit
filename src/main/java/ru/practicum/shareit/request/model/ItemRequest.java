package ru.practicum.shareit.request.model;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.practicum.shareit.user.model.User;

import java.util.Date;

@Data
@Accessors(chain = true)
public class ItemRequest {
    Long id;

    String description;

    User requestor;

    Date created;
}
