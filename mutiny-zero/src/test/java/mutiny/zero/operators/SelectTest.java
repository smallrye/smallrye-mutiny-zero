package mutiny.zero.operators;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
    @DisplayName("Filter elements")
    void maintainDemand() {
        Flow.Publisher<Integer> source = ZeroPublisher.fromItems(1, 2, 3, 4);
        Select<Integer> operator = new Select<>(source, n -> n % 2 == 0);

        AssertSubscriber<Object> sub = AssertSubscriber.create(3L);
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

    @Test
    @DisplayName("No error when cancel races with onNext on a rejecting predicate")
    void cancelRacingWithOnNextOnRejectingPredicate() throws InterruptedException {
        CountDownLatch inPredicate = new CountDownLatch(1);
        CountDownLatch cancelDone = new CountDownLatch(1);
        AtomicReference<Throwable> spuriousError = new AtomicReference<>();
        AtomicBoolean onNextDone = new AtomicBoolean();

        Flow.Publisher<Integer> source = subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                new Thread(() -> {
                    subscriber.onNext(1);
                    onNextDone.set(true);
                }).start();
            }

            @Override
            public void cancel() {
            }
        });

        Select<Integer> operator = new Select<>(source, n -> {
            inPredicate.countDown();
            try {
                cancelDone.await(5, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return false;
        });

        AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();

        operator.subscribe(new Flow.Subscriber<Integer>() {
            @Override
            public void onSubscribe(Flow.Subscription s) {
                subscriptionRef.set(s);
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(Integer item) {
            }

            @Override
            public void onError(Throwable throwable) {
                spuriousError.compareAndSet(null, throwable);
            }

            @Override
            public void onComplete() {
            }
        });

        assertTrue(inPredicate.await(5, TimeUnit.SECONDS));
        subscriptionRef.get().cancel();
        cancelDone.countDown();

        await().untilTrue(onNextDone);

        Throwable caught = spuriousError.get();
        assertTrue(caught == null,
                "Subscriber must not receive an error after cancel, but got: " + caught);
    }
}
