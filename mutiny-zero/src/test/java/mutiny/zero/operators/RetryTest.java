package mutiny.zero.operators;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.BackpressureStrategy;
import mutiny.zero.TubeConfiguration;
import mutiny.zero.ZeroPublisher;

@DisplayName("Retry operator tests")
class RetryTest {

    @Test
    @DisplayName("Check always predicate")
    void checkAlwaysPredicate() {
        IOException err = new IOException("boom");
        Predicate<Throwable> predicate = Retry.always();
        for (int i = 0; i < 10; i++) {
            assertTrue(predicate.test(err));
        }
    }

    @Test
    @DisplayName("Check atMost predicate")
    void checkAtMostPredicate() {
        IOException err = new IOException("boom");
        Predicate<Throwable> predicate = Retry.atMost(5);
        for (int i = 0; i < 5; i++) {
            assertTrue(predicate.test(err));
        }
        for (int i = 0; i < 5; i++) {
            assertFalse(predicate.test(err));
        }
    }

    @Test
    @DisplayName("Check atMost predicate")
    void checkAtMostPredicateRejectsBadCount() {
        assertThrows(IllegalArgumentException.class, () -> Retry.atMost(0));
        assertThrows(IllegalArgumentException.class, () -> Retry.atMost(-10));
    }

    @Test
    @DisplayName("Check that the operator does retry")
    void checkRetry() {
        TubeConfiguration tubeConfiguration = new TubeConfiguration()
                .withBackpressureStrategy(BackpressureStrategy.BUFFER)
                .withBufferSize(256);
        Flow.Publisher<String> source = ZeroPublisher.create(tubeConfiguration, tube -> {
            tube.send("Yolo");
            tube.fail(new IOException("boom"));
        });
        Retry<String> operator = new Retry<>(source, Retry.atMost(3));

        AssertSubscriber<String> sub = AssertSubscriber.create(Long.MAX_VALUE);
        operator.subscribe(sub);

        sub.assertFailedWith(IOException.class, "boom");
        assertIterableEquals(List.of("Yolo", "Yolo", "Yolo", "Yolo"), sub.getItems());
    }

    @Test
    @DisplayName("Handle throwing predicate")
    void handleThrowingPredicate() {
        TubeConfiguration tubeConfiguration = new TubeConfiguration()
                .withBackpressureStrategy(BackpressureStrategy.BUFFER)
                .withBufferSize(256);
        Flow.Publisher<String> source = ZeroPublisher.create(tubeConfiguration, tube -> {
            tube.send("Yolo");
            tube.fail(new IOException("boom"));
        });
        Retry<String> operator = new Retry<>(source, (err) -> {
            throw new RuntimeException("woops");
        });

        AssertSubscriber<String> sub = AssertSubscriber.create(Long.MAX_VALUE);
        operator.subscribe(sub);

        sub.assertFailedWith(RuntimeException.class, "woops");
    }

    @Test
    @DisplayName("Reject null upstream")
    void rejectNullUpstream() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> new Retry<>(null, Retry.always()));
        assertEquals("The upstream cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Reject null predicate")
    void rejectNullPredicate() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> new Retry<>(ZeroPublisher.empty(), null));
        assertEquals("The retry predicate cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("onSubscribe must be called exactly once even with retries (spec rule 2.12)")
    void onSubscribeCalledExactlyOnce() {
        Flow.Publisher<String> source = ZeroPublisher.fromFailure(new IOException("boom"));
        Retry<String> operator = new Retry<>(source, Retry.atMost(3));

        AtomicInteger onSubscribeCount = new AtomicInteger();
        AtomicReference<Throwable> receivedError = new AtomicReference<>();

        operator.subscribe(new Flow.Subscriber<String>() {
            @Override
            public void onSubscribe(Flow.Subscription s) {
                onSubscribeCount.incrementAndGet();
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
            }

            @Override
            public void onError(Throwable throwable) {
                receivedError.set(throwable);
            }

            @Override
            public void onComplete() {
            }
        });

        assertEquals(1, onSubscribeCount.get(),
                "onSubscribe must be called exactly once, but was called " + onSubscribeCount.get() + " times");
        assertNotNull(receivedError.get());
        assertInstanceOf(IOException.class, receivedError.get());
        assertEquals("boom", receivedError.get().getMessage());
    }
}
