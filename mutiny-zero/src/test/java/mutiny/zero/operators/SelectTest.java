package mutiny.zero.operators;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.Flow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.ZeroPublisher;

@DisplayName("Select operator tests")
class SelectTest {

    @Test
    @DisplayName("Filter elements")
    void filterElements() {
        Flow.Publisher<Integer> source = ZeroPublisher.fromItems(1, 2, 3, 4);
        Select<Integer> operator = new Select<>(source, n -> n % 2 == 0);

        AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
        operator.subscribe(sub);

        sub.assertCompleted().assertItems(2, 4);
    }

    @Test
    @DisplayName("Reject a null source")
    void rejectNullSource() {
        assertThatThrownBy(() -> new Select<>(null, o -> true))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Reject a null predicate")
    void rejectNullPredicate() {
        assertThatThrownBy(() -> new Select<>(ZeroPublisher.empty(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Handle exceptions thrown by a predicate")
    void handleThrowingPredicate() {
        Flow.Publisher<Integer> source = ZeroPublisher.fromItems(1, 2, 3, 4);
        Select<Integer> operator = new Select<>(source, n -> {
            throw new RuntimeException("yolo");
        });

        AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
        operator.subscribe(sub);

        sub.assertFailedWith(RuntimeException.class, "yolo");
    }
}
