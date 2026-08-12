package mutiny.zero.operators;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.ZeroPublisher;

@DisplayName("Tests for stream concatenation")
class ConcatenateTest {

    @Test
    @DisplayName("Concatenate 2 streams, unbounded demand")
    void concatenateFullSteam() {
        Flow.Publisher<Integer> a = ZeroPublisher.fromItems(1, 2, 3);
        Flow.Publisher<Integer> b = ZeroPublisher.fromItems(4, 5, 6);
        Concatenate<Integer> stream = new Concatenate<>(List.of(a, b));

        AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
        stream.subscribe(sub);

        sub.assertCompleted().assertItems(1, 2, 3, 4, 5, 6);
    }

    @Test
    @DisplayName("Concatenate 2 streams, paced demand")
    void concatenateFullPaced() {
        Flow.Publisher<Integer> a = ZeroPublisher.fromItems(1, 2, 3);
        Flow.Publisher<Integer> b = ZeroPublisher.fromItems(4, 5, 6);
        Concatenate<Integer> stream = new Concatenate<>(List.of(a, b));

        AssertSubscriber<Object> sub = AssertSubscriber.create();
        stream.subscribe(sub);

        sub.request(4);
        sub.assertItems(1, 2, 3, 4);
        sub.request(1);
        sub.assertItems(1, 2, 3, 4, 5);
        sub.request(100);

        sub.assertCompleted().assertItems(1, 2, 3, 4, 5, 6);
    }

    @Test
    @DisplayName("Concatenate 2 streams, one with an error")
    void concatenateErrored() {
        Flow.Publisher<Integer> a = ZeroPublisher.fromItems(1, 2, 3);
        Flow.Publisher<Integer> b = ZeroPublisher.fromFailure(new IOException("boom"));
        Concatenate<Integer> stream = new Concatenate<>(List.of(a, b));

        AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
        stream.subscribe(sub);

        sub.assertFailedWith(IOException.class, "boom");
        sub.assertItems(1, 2, 3);
    }

    @Test
    @DisplayName("Concatenate nothing")
    void concatenateEmpty() {
        Concatenate<Object> stream = new Concatenate<>(List.of());

        AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
        stream.subscribe(sub);

        sub.assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    @DisplayName("Reject null publishers")
    void rejectNullSources() {
        assertThrows(NullPointerException.class, () -> new Concatenate<>(null));
    }

    @Test
    @DisplayName("Reject null publisher in a list")
    void rejectListWithNullPublisher() {
        ArrayList<Flow.Publisher<Object>> list = new ArrayList<>();
        list.add(ZeroPublisher.empty());
        list.add(ZeroPublisher.empty());
        list.add(null);
        assertThrows(NullPointerException.class, () -> new Concatenate<>(list));
    }

    @Test
    @DisplayName("No error when cancel races with request during publisher transition")
    void cancelRacingWithRequestDuringTransition() throws InterruptedException {
        CountDownLatch inOnNext = new CountDownLatch(1);
        CountDownLatch cancelDone = new CountDownLatch(1);
        AtomicReference<Throwable> spuriousError = new AtomicReference<>();

        Flow.Publisher<Integer> asyncSource = subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                new Thread(() -> {
                    subscriber.onNext(1);
                    inOnNext.countDown();
                    try {
                        cancelDone.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    subscriber.onNext(2);
                    subscriber.onComplete();
                }).start();
            }

            @Override
            public void cancel() {
            }
        });

        Concatenate<Integer> stream = new Concatenate<>(List.of(asyncSource, ZeroPublisher.fromItems(3)));

        AtomicReference<Flow.Subscription> subscriptionRef = new AtomicReference<>();

        stream.subscribe(new Flow.Subscriber<Integer>() {
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

        assertTrue(inOnNext.await(5, TimeUnit.SECONDS));
        subscriptionRef.get().cancel();
        cancelDone.countDown();

        Thread.sleep(200);

        Throwable caught = spuriousError.get();
        assertTrue(caught == null,
                "Subscriber must not receive an error after cancel, but got: " + caught);
    }
}
