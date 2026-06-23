package ru.practicum.shareit;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.shareit.booking.BookingController;
import ru.practicum.shareit.booking.dto.BookingCreateDto;
import ru.practicum.shareit.booking.dto.BookingDto;
import ru.practicum.shareit.booking.service.BookingService;
import ru.practicum.shareit.item.ItemController;
import ru.practicum.shareit.item.dto.CommentCreateDto;
import ru.practicum.shareit.item.dto.CommentDto;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.dto.ItemOwnerDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.ItemRequestController;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.service.ItemRequestService;
import ru.practicum.shareit.user.UserController;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.time.LocalDateTime;
import java.util.List;

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
class ServerControllerMockMvcTest {
    private static final String USER_ID_HEADER = "X-Sharer-User-Id";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private ItemService itemService;

    @MockBean
    private BookingService bookingService;

    @MockBean
    private ItemRequestService itemRequestService;

    @Test
    void userControllerShouldCallServiceForAllEndpoints() throws Exception {
        UserDto user = new UserDto().setId(1L).setName("Ada").setEmail("ada@example.com");
        when(userService.create(any(UserDto.class))).thenReturn(user);
        when(userService.update(eq(1L), any(UserDto.class))).thenReturn(user.setName("Ada Lovelace"));
        when(userService.getById(1L)).thenReturn(user);
        when(userService.getAll()).thenReturn(List.of(user));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
        mockMvc.perform(patch("/users/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserDto().setName("Ada Lovelace"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ada Lovelace"));
        mockMvc.perform(get("/users/1")).andExpect(status().isOk());
        mockMvc.perform(get("/users")).andExpect(status().isOk());
        mockMvc.perform(delete("/users/1")).andExpect(status().isOk());

        verify(userService).delete(1L);
    }

    @Test
    void itemControllerShouldCallServiceForAllEndpoints() throws Exception {
        ItemDto item = new ItemDto()
                .setId(2L)
                .setName("Drill")
                .setDescription("Power drill")
                .setAvailable(true)
                .setOwnerId(1L);
        CommentDto comment = new CommentDto()
                .setId(3L)
                .setText("Good")
                .setAuthorName("Bob")
                .setCreated(LocalDateTime.of(2026, 6, 21, 12, 0));
        when(itemService.create(eq(1L), any(ItemDto.class))).thenReturn(item);
        when(itemService.update(eq(1L), eq(2L), any(ItemDto.class))).thenReturn(item.setName("Updated drill"));
        when(itemService.getById(1L, 2L)).thenReturn(item.setComments(List.of(comment)));
        when(itemService.getAllByOwnerId(1L)).thenReturn(List.of(new ItemOwnerDto()
                .setId(2L)
                .setName("Updated drill")
                .setDescription("Power drill")
                .setAvailable(true)
                .setOwnerId(1L)
                .setComments(List.of(comment))));
        when(itemService.search("drill")).thenReturn(List.of(item));
        when(itemService.addComment(eq(4L), eq(2L), any(CommentCreateDto.class))).thenReturn(comment);

        mockMvc.perform(post("/items")
                        .header(USER_ID_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
        mockMvc.perform(patch("/items/2")
                        .header(USER_ID_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ItemDto().setName("Updated drill"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated drill"));
        mockMvc.perform(get("/items/2").header(USER_ID_HEADER, 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comments[0].text").value("Good"));
        mockMvc.perform(get("/items").header(USER_ID_HEADER, 1)).andExpect(status().isOk());
        mockMvc.perform(get("/items/search").param("text", "drill")).andExpect(status().isOk());
        mockMvc.perform(post("/items/2/comment")
                        .header(USER_ID_HEADER, 4)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CommentCreateDto().setText("Good"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorName").value("Bob"));
    }

    @Test
    void bookingControllerShouldCallServiceForAllEndpoints() throws Exception {
        LocalDateTime start = LocalDateTime.of(2026, 6, 22, 10, 0);
        BookingDto booking = new BookingDto()
                .setId(5L)
                .setStart(start)
                .setEnd(start.plusHours(1))
                .setItem(new ItemDto().setId(2L).setName("Drill"))
                .setBooker(new UserDto().setId(3L).setName("Booker"));
        when(bookingService.create(eq(3L), any(BookingCreateDto.class))).thenReturn(booking);
        when(bookingService.updateApproval(1L, 5L, true)).thenReturn(booking);
        when(bookingService.getById(3L, 5L)).thenReturn(booking);
        when(bookingService.getAllByUserId(3L, "ALL")).thenReturn(List.of(booking));
        when(bookingService.getAllByOwnerId(1L, "WAITING")).thenReturn(List.of(booking));

        mockMvc.perform(post("/bookings")
                        .header(USER_ID_HEADER, 3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BookingCreateDto()
                                .setItemId(2L)
                                .setStart(start)
                                .setEnd(start.plusHours(1)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5));
        mockMvc.perform(patch("/bookings/5")
                        .header(USER_ID_HEADER, 1)
                        .param("approved", "true"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/bookings/5").header(USER_ID_HEADER, 3)).andExpect(status().isOk());
        mockMvc.perform(get("/bookings").header(USER_ID_HEADER, 3)).andExpect(status().isOk());
        mockMvc.perform(get("/bookings/owner")
                        .header(USER_ID_HEADER, 1)
                        .param("state", "WAITING"))
                .andExpect(status().isOk());
    }

    @Test
    void itemRequestControllerShouldCallServiceForAllEndpoints() throws Exception {
        ItemRequestDto request = new ItemRequestDto()
                .setId(6L)
                .setDescription("Need ladder")
                .setCreated(LocalDateTime.of(2026, 6, 21, 12, 0))
                .setItems(List.of());
        when(itemRequestService.create(eq(1L), any(ItemRequestDto.class))).thenReturn(request);
        when(itemRequestService.getAllByRequestor(1L)).thenReturn(List.of(request));
        when(itemRequestService.getAll(1L, 0, 20)).thenReturn(List.of(request));
        when(itemRequestService.getById(1L, 6L)).thenReturn(request);

        mockMvc.perform(post("/requests")
                        .header(USER_ID_HEADER, 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(6));
        mockMvc.perform(get("/requests").header(USER_ID_HEADER, 1)).andExpect(status().isOk());
        mockMvc.perform(get("/requests/all")
                        .header(USER_ID_HEADER, 1)
                        .param("from", "0")
                        .param("size", "20"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/requests/6").header(USER_ID_HEADER, 1)).andExpect(status().isOk());
    }
}
