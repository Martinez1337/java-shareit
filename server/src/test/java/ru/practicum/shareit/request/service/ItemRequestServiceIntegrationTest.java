package ru.practicum.shareit.request.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.item.dto.ItemDto;
import ru.practicum.shareit.item.service.ItemService;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.user.dto.UserDto;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class ItemRequestServiceIntegrationTest {
    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private UserService userService;

    @Test
    void createGetOwnGetAllAndGetByIdShouldReturnRequestsWithResponses() {
        UserDto requestor = createUser("requestor@example.com");
        UserDto owner = createUser("owner-request@example.com");
        ItemRequestDto request = itemRequestService.create(requestor.getId(), new ItemRequestDto()
                .setDescription("Need projector"));
        ItemDto responseItem = itemService.create(owner.getId(), new ItemDto()
                .setName("Projector")
                .setDescription("HD projector")
                .setAvailable(true)
                .setRequestId(request.getId()));

        List<ItemRequestDto> ownRequests = itemRequestService.getAllByRequestor(requestor.getId());
        List<ItemRequestDto> otherRequests = itemRequestService.getAll(owner.getId(), 0, 20);
        ItemRequestDto byId = itemRequestService.getById(owner.getId(), request.getId());

        assertThat(ownRequests).extracting(ItemRequestDto::getId).containsExactly(request.getId());
        assertThat(otherRequests).extracting(ItemRequestDto::getId).contains(request.getId());
        assertThat(byId.getItems())
                .extracting(ItemRequestResponseDto::getId)
                .containsExactly(responseItem.getId());
        assertThat(byId.getItems().getFirst().getOwnerId()).isEqualTo(owner.getId());
    }

    @Test
    void getAllShouldRejectInvalidPagination() {
        UserDto user = createUser("bad-page@example.com");

        assertThatThrownBy(() -> itemRequestService.getAll(user.getId(), -1, 20))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> itemRequestService.getAll(user.getId(), 0, 0))
                .isInstanceOf(BadRequestException.class);
    }

    private UserDto createUser(String email) {
        return userService.create(new UserDto()
                .setName(email.substring(0, email.indexOf('@')))
                .setEmail(email));
    }
}
