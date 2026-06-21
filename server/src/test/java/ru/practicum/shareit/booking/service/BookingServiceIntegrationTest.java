package ru.practicum.shareit.booking.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class BookingServiceIntegrationTest {
    @Autowired
    private BookingService bookingService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Test
    void createApproveAndGetByIdShouldPersistBookingWithRelations() {
        UserDto owner = createUser("owner-booking@example.com");
        UserDto booker = createUser("booker-booking@example.com");
        ItemDto item = createItem(owner.getId(), "Drill");
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        LocalDateTime end = start.plusHours(2);

        BookingDto created = bookingService.create(booker.getId(), new BookingCreateDto()
                .setItemId(item.getId())
                .setStart(start)
                .setEnd(end));

        assertThat(created.getStatus()).isEqualTo(BookingStatus.WAITING);
        assertThat(created.getBooker().getId()).isEqualTo(booker.getId());
        assertThat(created.getItem().getId()).isEqualTo(item.getId());

        BookingDto approved = bookingService.updateApproval(owner.getId(), created.getId(), true);

        assertThat(approved.getStatus()).isEqualTo(BookingStatus.APPROVED);
        assertThat(bookingService.getById(booker.getId(), created.getId()).getStatus())
                .isEqualTo(BookingStatus.APPROVED);
    }

    @Test
    void createShouldRejectOwnerBookingOwnItemAndInvalidDates() {
        UserDto owner = createUser("owner-invalid-booking@example.com");
        ItemDto item = createItem(owner.getId(), "Saw");
        LocalDateTime start = LocalDateTime.now().plusHours(2);

        assertThatThrownBy(() -> bookingService.create(owner.getId(), new BookingCreateDto()
                .setItemId(item.getId())
                .setStart(start)
                .setEnd(start.plusHours(1))))
                .isInstanceOf(NotFoundException.class);

        UserDto booker = createUser("booker-invalid-booking@example.com");
        assertThatThrownBy(() -> bookingService.create(booker.getId(), new BookingCreateDto()
                .setItemId(item.getId())
                .setStart(start)
                .setEnd(start)))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void getAllShouldFilterByBookerAndOwnerState() {
        UserDto owner = createUser("owner-state@example.com");
        UserDto booker = createUser("booker-state@example.com");
        ItemDto item = createItem(owner.getId(), "Tent");
        BookingDto waiting = bookingService.create(booker.getId(), new BookingCreateDto()
                .setItemId(item.getId())
                .setStart(LocalDateTime.now().plusDays(1))
                .setEnd(LocalDateTime.now().plusDays(2)));

        assertThat(bookingService.getAllByUserId(booker.getId(), "WAITING"))
                .extracting(BookingDto::getId)
                .contains(waiting.getId());
        assertThat(bookingService.getAllByOwnerId(owner.getId(), "WAITING"))
                .extracting(BookingDto::getId)
                .contains(waiting.getId());
    }

    @Test
    void createShouldRejectUnavailableItemMissingItemAndNullDates() {
        UserDto owner = createUser("owner-unavailable@example.com");
        UserDto booker = createUser("booker-unavailable@example.com");
        ItemDto item = itemService.create(owner.getId(), new ItemDto()
                .setName("Unavailable")
                .setDescription("Unavailable item")
                .setAvailable(false));
        LocalDateTime start = LocalDateTime.now().plusHours(1);

        assertThatThrownBy(() -> bookingService.create(booker.getId(), new BookingCreateDto()
                .setItemId(item.getId())
                .setStart(start)
                .setEnd(start.plusHours(1))))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> bookingService.create(booker.getId(), new BookingCreateDto()
                .setItemId(999_999L)
                .setStart(start)
                .setEnd(start.plusHours(1))))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> bookingService.create(booker.getId(), new BookingCreateDto()
                .setItemId(item.getId())
                .setEnd(start.plusHours(1))))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateApprovalAndGetByIdShouldRejectUnauthorizedAndRepeatedOperations() {
        UserDto owner = createUser("owner-approval@example.com");
        UserDto booker = createUser("booker-approval@example.com");
        UserDto stranger = createUser("stranger-approval@example.com");
        ItemDto item = createItem(owner.getId(), "Kayak");
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        BookingDto booking = bookingService.create(booker.getId(), new BookingCreateDto()
                .setItemId(item.getId())
                .setStart(start)
                .setEnd(start.plusHours(1)));

        assertThatThrownBy(() -> bookingService.updateApproval(stranger.getId(), booking.getId(), true))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> bookingService.getById(stranger.getId(), booking.getId()))
                .isInstanceOf(NotFoundException.class);

        BookingDto rejected = bookingService.updateApproval(owner.getId(), booking.getId(), false);
        assertThat(rejected.getStatus()).isEqualTo(BookingStatus.REJECTED);
        assertThatThrownBy(() -> bookingService.updateApproval(owner.getId(), booking.getId(), true))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> bookingService.getById(booker.getId(), 999_999L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getAllShouldSupportAllCurrentPastFutureRejectedAndBlankStates() {
        UserDto owner = createUser("owner-all-states@example.com");
        UserDto booker = createUser("booker-all-states@example.com");
        ItemDto item = createItem(owner.getId(), "Board");
        LocalDateTime now = LocalDateTime.now();
        BookingDto past = bookingService.create(booker.getId(), new BookingCreateDto()
                .setItemId(item.getId())
                .setStart(now.minusDays(2))
                .setEnd(now.minusDays(1)));
        bookingService.updateApproval(owner.getId(), past.getId(), true);
        BookingDto current = bookingService.create(booker.getId(), new BookingCreateDto()
                .setItemId(item.getId())
                .setStart(now.minusHours(1))
                .setEnd(now.plusHours(1)));
        bookingService.updateApproval(owner.getId(), current.getId(), true);
        BookingDto future = bookingService.create(booker.getId(), new BookingCreateDto()
                .setItemId(item.getId())
                .setStart(now.plusDays(1))
                .setEnd(now.plusDays(2)));
        bookingService.updateApproval(owner.getId(), future.getId(), true);
        BookingDto rejected = bookingService.create(booker.getId(), new BookingCreateDto()
                .setItemId(item.getId())
                .setStart(now.plusDays(3))
                .setEnd(now.plusDays(4)));
        bookingService.updateApproval(owner.getId(), rejected.getId(), false);

        assertThat(bookingService.getAllByUserId(booker.getId(), null))
                .extracting(BookingDto::getId)
                .contains(past.getId(), current.getId(), future.getId(), rejected.getId());
        assertThat(bookingService.getAllByUserId(booker.getId(), " "))
                .extracting(BookingDto::getId)
                .contains(past.getId(), current.getId(), future.getId(), rejected.getId());
        assertThat(bookingService.getAllByUserId(booker.getId(), "CURRENT"))
                .extracting(BookingDto::getId)
                .contains(current.getId());
        assertThat(bookingService.getAllByUserId(booker.getId(), "PAST"))
                .extracting(BookingDto::getId)
                .contains(past.getId());
        assertThat(bookingService.getAllByUserId(booker.getId(), "FUTURE"))
                .extracting(BookingDto::getId)
                .contains(future.getId(), rejected.getId());
        assertThat(bookingService.getAllByUserId(booker.getId(), "REJECTED"))
                .extracting(BookingDto::getId)
                .contains(rejected.getId());
        assertThat(bookingService.getAllByOwnerId(owner.getId(), "ALL"))
                .extracting(BookingDto::getId)
                .contains(past.getId(), current.getId(), future.getId(), rejected.getId());
        assertThat(bookingService.getAllByOwnerId(owner.getId(), "CURRENT"))
                .extracting(BookingDto::getId)
                .contains(current.getId());
        assertThat(bookingService.getAllByOwnerId(owner.getId(), "PAST"))
                .extracting(BookingDto::getId)
                .contains(past.getId());
        assertThat(bookingService.getAllByOwnerId(owner.getId(), "FUTURE"))
                .extracting(BookingDto::getId)
                .contains(future.getId(), rejected.getId());
        assertThat(bookingService.getAllByOwnerId(owner.getId(), "REJECTED"))
                .extracting(BookingDto::getId)
                .contains(rejected.getId());
        assertThatThrownBy(() -> bookingService.getAllByUserId(booker.getId(), "UNKNOWN"))
                .isInstanceOf(BadRequestException.class);
    }

    private UserDto createUser(String email) {
        return userService.create(new UserDto()
                .setName(email)
                .setEmail(email));
    }

    private ItemDto createItem(Long ownerId, String name) {
        return itemService.create(ownerId, new ItemDto()
                .setName(name)
                .setDescription(name + " description")
                .setAvailable(true));
    }
}
