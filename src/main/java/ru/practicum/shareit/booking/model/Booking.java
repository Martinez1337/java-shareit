package ru.practicum.shareit.booking.model;

import lombok.Data;
import lombok.experimental.Accessors;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.user.model.User;

import java.util.Date;

@Data
@Accessors(chain = true)
public class Booking {
    Long id;

    Date start;

    Date end;

    Item item;

    User booker;

    BookingStatus status;
}
