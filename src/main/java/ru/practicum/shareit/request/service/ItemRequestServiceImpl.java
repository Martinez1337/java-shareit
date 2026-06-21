package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.shareit.exception.BadRequestException;
import ru.practicum.shareit.exception.NotFoundException;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.item.repository.ItemRepository;
import ru.practicum.shareit.request.dto.ItemRequestDto;
import ru.practicum.shareit.request.dto.ItemRequestMapper;
import ru.practicum.shareit.request.dto.ItemRequestResponseDto;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.request.repository.ItemRequestRepository;
import ru.practicum.shareit.user.model.User;
import ru.practicum.shareit.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemRequestServiceImpl implements ItemRequestService {
    private final ItemRequestRepository itemRequestRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final ItemRequestMapper itemRequestMapper;

    @Override
    @Transactional
    public ItemRequestDto create(Long userId, ItemRequestDto itemRequestDto) {
        User requestor = getUser(userId);
        ItemRequest itemRequest = itemRequestMapper.map(itemRequestDto)
                .setRequestor(requestor)
                .setCreated(LocalDateTime.now());

        return itemRequestMapper.mapToDto(itemRequestRepository.save(itemRequest))
                .setItems(List.of());
    }

    @Override
    public List<ItemRequestDto> getAllByRequestor(Long userId) {
        getUser(userId);
        List<ItemRequest> requests = itemRequestRepository.findAllByRequestor_IdOrderByCreatedDesc(userId);
        return mapToDtos(requests);
    }

    @Override
    public List<ItemRequestDto> getAll(Long userId, Integer from, Integer size) {
        getUser(userId);
        List<ItemRequest> requests = getOtherUsersRequests(userId, from, size);
        return mapToDtos(requests);
    }

    @Override
    public ItemRequestDto getById(Long userId, Long requestId) {
        getUser(userId);
        ItemRequest itemRequest = getRequest(requestId);
        Map<Long, List<ItemRequestResponseDto>> itemsByRequestId = getItemsByRequestId(List.of(requestId));
        return mapToDto(itemRequest, itemsByRequestId);
    }

    private List<ItemRequestDto> mapToDtos(List<ItemRequest> requests) {
        List<Long> requestIds = requests.stream()
                .map(ItemRequest::getId)
                .toList();
        Map<Long, List<ItemRequestResponseDto>> itemsByRequestId = getItemsByRequestId(requestIds);

        return requests.stream()
                .map(request -> mapToDto(request, itemsByRequestId))
                .toList();
    }

    private ItemRequestDto mapToDto(
            ItemRequest itemRequest,
            Map<Long, List<ItemRequestResponseDto>> itemsByRequestId
    ) {
        return itemRequestMapper.mapToDto(itemRequest)
                .setItems(itemsByRequestId.getOrDefault(itemRequest.getId(), List.of()));
    }

    private Map<Long, List<ItemRequestResponseDto>> getItemsByRequestId(List<Long> requestIds) {
        if (requestIds.isEmpty()) {
            return Map.of();
        }

        return itemRepository.findAllByRequest_IdIn(requestIds)
                .stream()
                .collect(Collectors.groupingBy(
                        item -> item.getRequest().getId(),
                        Collectors.mapping(this::mapToResponseDto, Collectors.toList())
                ));
    }

    private ItemRequestResponseDto mapToResponseDto(Item item) {
        return itemRequestMapper.mapToResponseDto(item);
    }

    private List<ItemRequest> getOtherUsersRequests(Long userId, Integer from, Integer size) {
        if (from == null && size == null) {
            return itemRequestRepository.findAllByRequestor_IdNotOrderByCreatedDesc(userId);
        }

        int actualFrom = from == null ? 0 : from;
        int actualSize = size == null ? 20 : size;
        if (actualFrom < 0 || actualSize <= 0) {
            throw new BadRequestException("Invalid pagination parameters");
        }

        return itemRequestRepository.findAllByRequestorIdNot(userId, actualFrom, actualSize);
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found: " + userId));
    }

    private ItemRequest getRequest(Long requestId) {
        return itemRequestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Item request not found: " + requestId));
    }
}
