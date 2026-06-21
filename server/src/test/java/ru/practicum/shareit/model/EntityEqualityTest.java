package ru.practicum.shareit.model;

import org.junit.jupiter.api.Test;
import ru.practicum.shareit.booking.model.Booking;
import ru.practicum.shareit.item.model.Comment;
import ru.practicum.shareit.item.model.Item;
import ru.practicum.shareit.request.model.ItemRequest;
import ru.practicum.shareit.user.model.User;

import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

class EntityEqualityTest {

    @Test
    void userEqualsAndHashCodeShouldUseNonNullId() {
        assertEntityEquality(id -> new User().setId(id));
    }

    @Test
    void itemEqualsAndHashCodeShouldUseNonNullId() {
        assertEntityEquality(id -> new Item().setId(id));
    }

    @Test
    void bookingEqualsAndHashCodeShouldUseNonNullId() {
        assertEntityEquality(id -> new Booking().setId(id));
    }

    @Test
    void commentEqualsAndHashCodeShouldUseNonNullId() {
        assertEntityEquality(id -> new Comment().setId(id));
    }

    @Test
    void itemRequestEqualsAndHashCodeShouldUseNonNullId() {
        assertEntityEquality(id -> new ItemRequest().setId(id));
    }

    private <T> void assertEntityEquality(EntityFactory<T> factory) {
        T entity = factory.create(1L);
        T sameId = factory.create(1L);
        T otherId = factory.create(2L);
        T withoutId = factory.create(null);

        assertThat(entity).isEqualTo(entity);
        assertThat(entity).isEqualTo(sameId);
        assertThat(entity).isNotEqualTo(otherId);
        assertThat(entity).isNotEqualTo(withoutId);
        assertThat(withoutId).isNotEqualTo(factory.create(null));
        assertThat(entity).isNotEqualTo("not entity");
        assertThat(entity).hasSameHashCodeAs(sameId);
    }

    @FunctionalInterface
    private interface EntityFactory<T> extends Supplier<T> {
        T create(Long id);

        @Override
        default T get() {
            return create(1L);
        }
    }
}
