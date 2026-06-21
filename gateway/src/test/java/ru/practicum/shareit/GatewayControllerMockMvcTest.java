package ru.practicum.shareit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.BookingClient;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.item.ItemClient;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.request.ItemRequestClient;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.user.UserClient;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.dto.UserDto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        UserController.class,
        ItemController.class,
        BookingController.class,
        ItemRequestController.class
})
class GatewayControllerMockMvcTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserClient userClient;

    @MockBean
    private ItemClient itemClient;

    @MockBean
    private BookingClient bookingClient;

    @MockBean
    private ItemRequestClient itemRequestClient;

    @Test
    void userEndpointsShouldDelegateToClientAndValidateInput() throws Exception {
        UserDto createDto = new UserDto().setName("Ada").setEmail("ada@example.com");
        UserDto patchDto = new UserDto().setName("Ada Lovelace");
        when(userClient.create(any(UserDto.class))).thenReturn(ResponseEntity.status(201).body(Map.of("id", 1)));
        when(userClient.update(eq(1L), any(UserDto.class))).thenReturn(ResponseEntity.ok(Map.of("id", 1)));
        when(userClient.getById(1L)).thenReturn(ResponseEntity.ok(Map.of("id", 1)));
        when(userClient.getAll()).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 1))));
        when(userClient.delete(1L)).thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(patchDto)))
                .andExpect(status().isOk());
        mockMvc.perform(get("/users/1")).andExpect(status().isOk());
        mockMvc.perform(get("/users")).andExpect(status().isOk());
        mockMvc.perform(delete("/users/1")).andExpect(status().isNoContent());

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserDto().setName("").setEmail("bad"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void itemEndpointsShouldDelegateToClientAndValidateInput() throws Exception {
        ItemDto itemDto = new ItemDto()
                .setName("Drill")
                .setDescription("Power drill")
                .setAvailable(true)
                .setRequestId(10L);
        CommentCreateDto commentDto = new CommentCreateDto().setText("Good item");
        when(itemClient.create(eq(1L), any(ItemDto.class))).thenReturn(ResponseEntity.status(201).body(Map.of("id", 2)));
        when(itemClient.update(eq(1L), eq(2L), any(ItemDto.class))).thenReturn(ResponseEntity.ok(Map.of("id", 2)));
        when(itemClient.getById(1L, 2L)).thenReturn(ResponseEntity.ok(Map.of("id", 2)));
        when(itemClient.getByOwner(1L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 2))));
        when(itemClient.search("drill")).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 2))));
        when(itemClient.addComment(eq(3L), eq(2L), any(CommentCreateDto.class)))
                .thenReturn(ResponseEntity.ok(Map.of("id", 4)));

        mockMvc.perform(post("/items")
                        .header(USER_ID_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(itemDto)))
                .andExpect(status().isCreated());
        mockMvc.perform(patch("/items/2")
                        .header(USER_ID_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemDto().setName("New drill"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/2").header(USER_ID_HEADER, 1)).andExpect(status().isOk());
        mockMvc.perform(get("/items").header(USER_ID_HEADER, 1)).andExpect(status().isOk());
        mockMvc.perform(get("/items/search").param("text", "drill")).andExpect(status().isOk());
        mockMvc.perform(post("/items/2/comment")
                        .header(USER_ID_HEADER, 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(commentDto)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/items")
                        .header(USER_ID_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemDto().setName("No description"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void bookingEndpointsShouldDelegateToClientAndValidateStateAndDates() throws Exception {
        LocalDateTime start = LocalDateTime.now().plusHours(1);
        BookingCreateDto bookingDto = new BookingCreateDto()
                .setItemId(2L)
                .setStart(start)
                .setEnd(start.plusHours(1));
        when(bookingClient.create(eq(3L), any(BookingCreateDto.class)))
                .thenReturn(ResponseEntity.status(201).body(Map.of("id", 5)));
        when(bookingClient.updateApproval(1L, 5L, true)).thenReturn(ResponseEntity.ok(Map.of("id", 5)));
        when(bookingClient.getById(3L, 5L)).thenReturn(ResponseEntity.ok(Map.of("id", 5)));
        when(bookingClient.getAllByUserId(3L, "ALL")).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 5))));
        when(bookingClient.getAllByOwnerId(1L, "WAITING")).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 5))));

        mockMvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bookingDto)))
                .andExpect(status().isCreated());
        mockMvc.perform(patch("/bookings/5")
                        .header(USER_ID_HEADER, 1)
                        .param("approved", "true"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings/5").header(USER_ID_HEADER, 3)).andExpect(status().isOk());
        mockMvc.perform(get("/bookings").header(USER_ID_HEADER, 3)).andExpect(status().isOk());
        mockMvc.perform(get("/bookings/owner")
                        .header(USER_ID_HEADER, 1)
                        .param("state", "waiting"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/bookings")
                        .header(USER_ID_HEADER, 3)
                        .param("state", "unknown"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto()
                                .setItemId(2L)
                                .setStart(start)
                                .setEnd(start))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requestEndpointsShouldDelegateToClientAndValidateInput() throws Exception {
        ItemRequestDto requestDto = new ItemRequestDto().setDescription("Need ladder");
        when(itemRequestClient.create(eq(1L), any(ItemRequestDto.class)))
                .thenReturn(ResponseEntity.status(201).body(Map.of("id", 6)));
        when(itemRequestClient.getAllByRequestor(1L)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 6))));
        when(itemRequestClient.getAll(1L, 0, 20)).thenReturn(ResponseEntity.ok(List.of(Map.of("id", 7))));
        when(itemRequestClient.getById(1L, 6L)).thenReturn(ResponseEntity.ok(Map.of("id", 6)));

        mockMvc.perform(post("/requests")
                        .header(USER_ID_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated());
        mockMvc.perform(get("/requests").header(USER_ID_HEADER, 1)).andExpect(status().isOk());
        mockMvc.perform(get("/requests/all").header(USER_ID_HEADER, 1)).andExpect(status().isOk());
        mockMvc.perform(get("/requests/6").header(USER_ID_HEADER, 1)).andExpect(status().isOk());

        mockMvc.perform(post("/requests")
                        .header(USER_ID_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemRequestDto().setDescription(""))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get("/requests/all")
                        .header(USER_ID_HEADER, 1)
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(itemRequestClient).getAll(1L, 0, 20);
    }
}
