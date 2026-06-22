package ru.practicum.shareit.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.request.model.ItemRequest;

import java.util.List;

public interface ItemRequestRepository extends JpaRepository<ItemRequest, Long> {

    List<ItemRequest> findAllByRequestor_IdOrderByCreatedDesc(Long requestorId);

    @Query(value = """
            SELECT *
            FROM requests r
            WHERE r.requestor_id <> :requestorId
            ORDER BY r.created DESC
            LIMIT :size OFFSET :from
            """, nativeQuery = true)
    List<ItemRequest> findAllByRequestorIdNot(
            @Param("requestorId") Long requestorId,
            @Param("from") int from,
            @Param("size") int size
    );
}
