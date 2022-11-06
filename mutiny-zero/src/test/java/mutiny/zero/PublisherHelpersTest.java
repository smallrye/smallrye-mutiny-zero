package mutiny.zero;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PublisherHelpersTest {

    @Nested
    @DisplayName("Collect all items to a list")
    class ToList {

        @Test
        @DisplayName("Collect items")
        void collect() {
            AtomicReference<List<Integer>> items = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            PublisherHelpers
                    .collectToList(ZeroPublisher.fromItems(1, 2, 3))
                    .whenComplete((list, err) -> {
                        items.set(list);
                        error.set(err);
                    });

            assertNotNull(items.get());
            assertIterableEquals(Arrays.asList(1, 2, 3), items.get());
            assertNull(error.get());
        }

        @Test
        @DisplayName("Collect error")
        void collectError() {
            AtomicReference<List<Object>> items = new AtomicReference<>();
            AtomicReference<Throwable> error = new AtomicReference<>();

            PublisherHelpers
                    .collectToList(ZeroPublisher.fromFailure(new IOException("boom")))
                    .whenComplete((list, err) -> {
                        items.set(list);
                        error.set(err);
                    });

            assertNull(items.get());
            assertNotNull(error.get());
            assertTrue(error.get() instanceof IOException);
            assertEquals("boom", error.get().getMessage());
        }
    }
}
