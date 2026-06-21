package ru.practicum.shareit.booking.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.item.dto.ItemMapper;
import ru.practicum.shareit.user.dto.UserMapper;

@Mapper(componentModel = "spring",
        uses = {ItemMapper.class, UserMapper.class})
public interface BookingMapper {

    BookingDto mapToDto(Booking booking);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "item", ignore = true)
    @Mapping(target = "booker", ignore = true)
    @Mapping(target = "status", ignore = true)
    Booking map(BookingCreateDto bookingCreateDto);
}
