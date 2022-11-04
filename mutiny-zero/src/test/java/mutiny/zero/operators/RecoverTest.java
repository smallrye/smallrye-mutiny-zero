package mutiny.zero.operators;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.concurrent.Flow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.BackpressureStrategy;
import mutiny.zero.TubeConfiguration;
import mutiny.zero.ZeroPublisher;

@DisplayName("Recover operator tests")
class RecoverTest {

    @Test
    @DisplayName("Recover from a failure")
    void recover() {
        Flow.Publisher<String> boom = ZeroPublisher.fromFailure(new IOException("boom"));
        Recover<String> operator = new Recover<>(boom, err -> "Yolo");

        AssertSubscriber<String> sub = AssertSubscriber.create(Long.MAX_VALUE);
        operator.subscribe(sub);

        sub.assertCompleted().assertItems("Yolo");
    }

    @Test
    @DisplayName("Recover from an emitter stream")
    void recoverFromEmitter() {
        TubeConfiguration tubeConfiguration = new TubeConfiguration().withBackpressureStrategy(BackpressureStrategy.BUFFER)
                .withBufferSize(256);
        Flow.Publisher<String> publisher = ZeroPublisher.create(tubeConfiguration, tube -> {
            tube.send("Yolo");
            tube.send("as a Service");
            tube.fail(new IOException("boom"));
        });
        Recover<String> operator = new Recover<>(publisher, err -> "!");

        AssertSubscriber<String> sub = AssertSubscriber.create(Long.MAX_VALUE);
        operator.subscribe(sub);

        sub.assertCompleted().assertItems("Yolo", "as a Service", "!");
    }

    @Test
    @DisplayName("Reject a null source")
    void rejectNullSource() {
        assertThatThrownBy(() -> new Recover<>(null, err -> "?"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Reject a null function")
    void rejectNullFunction() {
        assertThatThrownBy(() -> new Recover<>(ZeroPublisher.empty(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Handle exceptions thrown by a function")
    void handleThrowingFunction() {
        Flow.Publisher<Object> source = ZeroPublisher.fromFailure(new IOException("boom"));
        Recover<Object> operator = new Recover<>(source, err -> {
            throw new RuntimeException("yolo");
        });

        AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
        operator.subscribe(sub);

        sub.assertFailedWith(RuntimeException.class, "yolo");
    }

    @Test
    @DisplayName("Returning null from a function completes the stream")
    void returningNullTerminatesTheStream() {
        Flow.Publisher<Object> source = ZeroPublisher.fromFailure(new IOException("boom"));
        Recover<Object> operator = new Recover<>(source, err -> null);

        AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
        operator.subscribe(sub);

        sub.assertCompleted().assertHasNotReceivedAnyItem();
    }
}
