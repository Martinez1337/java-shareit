package ru.practicum.shareit.item.model;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

@Data
@Accessors(chain = true)
public class Item {
    Long id;

    String name;

    String description;

    Boolean available;

    User owner;

    ItemRequest request;
}
