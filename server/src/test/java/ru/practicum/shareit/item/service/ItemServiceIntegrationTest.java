package ru.practicum.shareit.item.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;
import ru.practicum.shareit.booking.repository.BookingRepository;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemOwnerDto;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ItemServiceIntegrationTest {
    private static final ZoneId APP_ZONE = ZoneId.of(System.getenv().getOrDefault("TZ", "Europe/Moscow"));

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Test
    void createUpdateSearchAndGetByIdShouldWorkWithRequestId() {
        UserDto owner = createUser("owner-item@example.com");
        UserDto requestor = createUser("requestor-item@example.com");
        ItemRequestDto request = itemRequestService.create(requestor.getId(), new ItemRequestDto()
                .setDescription("Need a ladder"));

        ItemDto created = itemService.create(owner.getId(), new ItemDto()
                .setName("Ladder")
                .setDescription("Tall aluminium ladder")
                .setAvailable(true)
                .setRequestId(request.getId()));

        assertThat(created.getRequestId()).isEqualTo(request.getId());
        assertThat(itemService.search("aluminium")).extracting(ItemDto::getId).contains(created.getId());

        ItemDto updated = itemService.update(owner.getId(), created.getId(), new ItemDto()
                .setName("Long ladder")
                .setAvailable(false));

        assertThat(updated.getName()).isEqualTo("Long ladder");
        assertThat(updated.getDescription()).isEqualTo("Tall aluminium ladder");
        assertThat(updated.getAvailable()).isFalse();

        assertThatThrownBy(() -> itemService.update(requestor.getId(), created.getId(), new ItemDto()
                .setName("Forbidden")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addCommentShouldRequireCompletedApprovedBookingAndReturnCommentInItem() {
        UserDto owner = createUser("owner-comment@example.com");
        UserDto booker = createUser("booker-comment@example.com");
        ItemDto item = itemService.create(owner.getId(), new ItemDto()
                .setName("Camera")
                .setDescription("Digital camera")
                .setAvailable(true));

        assertThatThrownBy(() -> itemService.addComment(booker.getId(), item.getId(), new CommentCreateDto()
                .setText("Too early")))
                .isInstanceOf(BadRequestException.class);

        createApprovedBooking(item.getId(), booker.getId(), LocalDateTime.now(APP_ZONE).minusDays(2));

        CommentDto comment = itemService.addComment(booker.getId(), item.getId(), new CommentCreateDto()
                .setText("Works well"));

        assertThat(comment.getId()).isNotNull();
        assertThat(comment.getAuthorName()).isEqualTo(booker.getName());
        assertThat(itemService.getById(booker.getId(), item.getId()).getComments())
                .extracting(CommentDto::getText)
                .containsExactly("Works well");
    }

    @Test
    void getAllByOwnerShouldIncludeLastNextBookingsAndComments() {
        UserDto owner = createUser("owner-dashboard@example.com");
        UserDto booker = createUser("booker-dashboard@example.com");
        ItemDto item = itemService.create(owner.getId(), new ItemDto()
                .setName("Bike")
                .setDescription("City bike")
                .setAvailable(true));
        LocalDateTime now = LocalDateTime.now(APP_ZONE);
        LocalDateTime pastStart = now.minusDays(3);
        LocalDateTime futureStart = now.plusDays(1);
        createApprovedBooking(item.getId(), booker.getId(), pastStart);
        createApprovedBooking(item.getId(), booker.getId(), futureStart);
        itemService.addComment(booker.getId(), item.getId(), new CommentCreateDto().setText("Good bike"));

        List<ItemOwnerDto> ownerItems = itemService.getAllByOwnerId(owner.getId());

        assertThat(ownerItems).hasSize(1);
        ItemOwnerDto ownerItem = ownerItems.getFirst();
        assertThat(ownerItem.getLastBooking()).isEqualTo(pastStart.plusHours(1));
        assertThat(ownerItem.getNextBooking()).isEqualTo(futureStart);
        assertThat(ownerItem.getComments()).extracting(CommentDto::getText).containsExactly("Good bike");
    }

    private UserDto createUser(String email) {
        return userService.create(new UserDto()
                .setName(email.substring(0, email.indexOf('@')))
                .setEmail(email));
    }

    private void createApprovedBooking(Long itemId, Long bookerId, LocalDateTime start) {
        User booker = userRepository.findById(bookerId).orElseThrow();
        Item item = itemRepository.findById(itemId).orElseThrow();
        bookingRepository.save(new Booking()
                .setItem(item)
                .setBooker(booker)
                .setStart(start)
                .setEnd(start.plusHours(1))
                .setStatus(BookingStatus.APPROVED));
    }
}
