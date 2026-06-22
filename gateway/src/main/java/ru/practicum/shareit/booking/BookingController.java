package ru.practicum.shareit.booking;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingState;
import ru.practicum.shareit.exception.BadRequestException;

import java.util.Locale;

@RestController
@RequestMapping(path = "/bookings")
@RequiredArgsConstructor
@Validated
public class BookingController {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    private final BookingClient bookingClient;

    @PostMapping
    public ResponseEntity<Object> create(
            @Positive @RequestHeader(USER_ID_HEADER) Long userId,
            @Valid @RequestBody BookingCreateDto bookingCreateDto
    ) {
        return bookingClient.create(userId, bookingCreateDto);
    }

    @PatchMapping("/{bookingId}")
    public ResponseEntity<Object> updateApproval(
            @Positive @RequestHeader(USER_ID_HEADER) Long userId,
            @Positive @PathVariable Long bookingId,
            @RequestParam Boolean approved
    ) {
        return bookingClient.updateApproval(userId, bookingId, approved);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<Object> getById(
            @Positive @RequestHeader(USER_ID_HEADER) Long userId,
            @Positive @PathVariable Long bookingId
    ) {
        return bookingClient.getById(userId, bookingId);
    }

    @GetMapping
    public ResponseEntity<Object> getAllByUserId(
            @Positive @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "ALL") String state
    ) {
        return bookingClient.getAllByUserId(userId, parseState(state));
    }

    @GetMapping("/owner")
    public ResponseEntity<Object> getAllByOwnerId(
            @Positive @RequestHeader(USER_ID_HEADER) Long userId,
            @RequestParam(defaultValue = "ALL") String state
    ) {
        return bookingClient.getAllByOwnerId(userId, parseState(state));
    }

    private String parseState(String state) {
        try {
            return BookingState.valueOf(state.trim().toUpperCase(Locale.ROOT)).name();
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("Unknown state: " + state);
        }
    }
}
