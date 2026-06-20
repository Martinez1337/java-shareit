package ru.practicum.shareit.booking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.booking.model.BookingStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    List<Booking> findAllByBooker_IdOrderByStartDesc(Long bookerId);

    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.booker.id = :bookerId
              AND b.start <= :now
              AND b.end >= :now
            ORDER BY b.start DESC
            """)
    List<Booking> findCurrentByBookerId(@Param("bookerId") Long bookerId, @Param("now") LocalDateTime now);

    List<Booking> findAllByBooker_IdAndEndBeforeOrderByStartDesc(Long bookerId, LocalDateTime now);

    List<Booking> findAllByBooker_IdAndStartAfterOrderByStartDesc(Long bookerId, LocalDateTime now);

    List<Booking> findAllByBooker_IdAndStatusOrderByStartDesc(Long bookerId, BookingStatus status);

    List<Booking> findAllByItem_IdInAndStatusOrderByStartDesc(List<Long> itemIds, BookingStatus status);

    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.item.owner.id = :ownerId
            ORDER BY b.start DESC
            """)
    List<Booking> findAllByOwnerId(@Param("ownerId") Long ownerId);

    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.item.owner.id = :ownerId
              AND b.start <= :now
              AND b.end >= :now
            ORDER BY b.start DESC
            """)
    List<Booking> findCurrentByOwnerId(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.item.owner.id = :ownerId
              AND b.end < :now
            ORDER BY b.start DESC
            """)
    List<Booking> findPastByOwnerId(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.item.owner.id = :ownerId
              AND b.start > :now
            ORDER BY b.start DESC
            """)
    List<Booking> findFutureByOwnerId(@Param("ownerId") Long ownerId, @Param("now") LocalDateTime now);

    @Query("""
            SELECT b
            FROM Booking b
            WHERE b.item.owner.id = :ownerId
              AND b.status = :status
            ORDER BY b.start DESC
            """)
    List<Booking> findAllByOwnerIdAndStatus(@Param("ownerId") Long ownerId, @Param("status") BookingStatus status);
}
