package ru.practicum.shareit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class DtoJsonTest {
    @Autowired
    private JacksonTester<BookingCreateDto> bookingJson;

    @Autowired
    private JacksonTester<ItemDto> itemJson;

    @Autowired
    private JacksonTester<ItemRequestDto> requestJson;

    @Test
    void bookingCreateDtoShouldSerializeAndDeserializeIsoLocalDateTime() throws Exception {
        BookingCreateDto dto = new BookingCreateDto()
                .setItemId(10L)
                .setStart(LocalDateTime.of(2026, 6, 21, 10, 15, 30))
                .setEnd(LocalDateTime.of(2026, 6, 21, 11, 15, 30));

        assertThat(bookingJson.write(dto)).extractingJsonPathStringValue("$.start")
                .isEqualTo("2026-06-21T10:15:30");
        assertThat(bookingJson.write(dto)).extractingJsonPathStringValue("$.end")
                .isEqualTo("2026-06-21T11:15:30");

        BookingCreateDto parsed = bookingJson.parse("""
                {
                  "itemId": 10,
                  "start": "2026-06-21T10:15:30",
                  "end": "2026-06-21T11:15:30"
                }
                """).getObject();

        assertThat(parsed.getItemId()).isEqualTo(10L);
        assertThat(parsed.getStart()).isEqualTo(dto.getStart());
        assertThat(parsed.getEnd()).isEqualTo(dto.getEnd());
    }

    @Test
    void itemDtoShouldSerializeNestedCommentsAndRequestId() throws Exception {
        ItemDto dto = new ItemDto()
                .setId(1L)
                .setName("Camera")
                .setDescription("Digital camera")
                .setAvailable(true)
                .setOwnerId(2L)
                .setRequestId(3L)
                .setComments(List.of(new CommentDto()
                        .setId(4L)
                        .setText("Good")
                        .setAuthorName("Alice")
                        .setCreated(LocalDateTime.of(2026, 6, 21, 12, 0))));

        assertThat(itemJson.write(dto)).extractingJsonPathNumberValue("$.requestId")
                .isEqualTo(3);
        assertThat(itemJson.write(dto)).extractingJsonPathStringValue("$.comments[0].created")
                .isEqualTo("2026-06-21T12:00:00");
        assertThat(itemJson.write(dto)).extractingJsonPathStringValue("$.comments[0].authorName")
                .isEqualTo("Alice");
    }

    @Test
    void itemRequestDtoShouldSerializeResponseItems() throws Exception {
        ItemRequestDto dto = new ItemRequestDto()
                .setId(1L)
                .setDescription("Need projector")
                .setCreated(LocalDateTime.of(2026, 6, 21, 13, 30))
                .setItems(List.of(new ItemRequestResponseDto()
                        .setId(2L)
                        .setName("Projector")
                        .setOwnerId(3L)));

        assertThat(requestJson.write(dto)).extractingJsonPathStringValue("$.created")
                .isEqualTo("2026-06-21T13:30:00");
        assertThat(requestJson.write(dto)).extractingJsonPathStringValue("$.items[0].name")
                .isEqualTo("Projector");
        assertThat(requestJson.write(dto)).extractingJsonPathNumberValue("$.items[0].ownerId")
                .isEqualTo(3);
    }
}
