package mutiny.zero.operators;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import mutiny.zero.ZeroPublisher;

@DisplayName("Tests for the Spread operator")
class SpreadTest {

    // ---- Basic functionality ---- //

    @Test
    @DisplayName("Spread with concurrency=1 preserves order (concatMap)")
    void concatMapOrdering() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2, 3);
        Spread<Integer, String> spread = new Spread<>(upstream,
                i -> ZeroPublisher.fromItems(i + "a", i + "b"), 1, 8);

        AssertSubscriber<String> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertCompleted().assertItems("1a", "1b", "2a", "2b", "3a", "3b");
    }

    @Test
    @DisplayName("Spread with concurrency > 1 delivers all items")
    void concurrentSpreadDeliversAll() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2, 3);
        Spread<Integer, String> spread = new Spread<>(upstream,
                i -> ZeroPublisher.fromItems(i + "a", i + "b"), 4, 8);

        AssertSubscriber<String> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertCompleted();
        List<String> items = sub.getItems();
        assertEquals(6, items.size());
        assertTrue(items.containsAll(List.of("1a", "1b", "2a", "2b", "3a", "3b")));
    }

    @Test
    @DisplayName("Empty upstream completes immediately")
    void emptyUpstream() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.empty();
        Spread<Integer, Integer> spread = new Spread<>(upstream, ZeroPublisher::fromItems, 1, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    @DisplayName("Empty inner publishers produce no items")
    void emptyInnerPublishers() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2, 3);
        Spread<Integer, Integer> spread = new Spread<>(upstream, i -> ZeroPublisher.empty(), 1, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertCompleted().assertHasNotReceivedAnyItem();
    }

    @Test
    @DisplayName("Mix of empty and non-empty inner publishers")
    void mixedEmptyAndNonEmpty() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2, 3);
        Spread<Integer, String> spread = new Spread<>(upstream,
                i -> (i % 2 == 0) ? ZeroPublisher.empty() : ZeroPublisher.fromItems(i + "x"), 1, 8);

        AssertSubscriber<String> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertCompleted().assertItems("1x", "3x");
    }

    // ---- Backpressure ---- //

    @Test
    @DisplayName("Paced demand (request one at a time)")
    void pacedDemand() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2);
        Spread<Integer, String> spread = new Spread<>(upstream,
                i -> ZeroPublisher.fromItems(i + "a", i + "b"), 1, 8);

        AssertSubscriber<String> sub = AssertSubscriber.create();
        spread.subscribe(sub);

        sub.request(1);
        sub.assertItems("1a");

        sub.request(1);
        sub.assertItems("1a", "1b");

        sub.request(1);
        sub.assertItems("1a", "1b", "2a");

        sub.request(1);
        sub.assertItems("1a", "1b", "2a", "2b");

        sub.request(1);
        sub.assertCompleted();
    }

    @Test
    @DisplayName("Unbounded demand (Long.MAX_VALUE)")
    void unboundedDemand() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2, 3);
        Spread<Integer, Integer> spread = new Spread<>(upstream,
                i -> ZeroPublisher.fromItems(i * 10, i * 100), 2, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertCompleted();
        assertEquals(6, sub.getItems().size());
    }

    // ---- Error handling ---- //

    @Test
    @DisplayName("Mapper function throws")
    void mapperThrows() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2, 3);
        Spread<Integer, Integer> spread = new Spread<>(upstream,
                i -> {
                    if (i == 2) {
                        throw new RuntimeException("mapper boom");
                    }
                    return ZeroPublisher.fromItems(i);
                }, 1, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertFailedWith(RuntimeException.class, "mapper boom");
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    @DisplayName("Mapper returns null")
    void mapperReturnsNull() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1);
        Spread<Integer, Integer> spread = new Spread<>(upstream, i -> null, 1, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertFailedWith(NullPointerException.class, null);
    }

    @Test
    @DisplayName("Inner publisher error propagates downstream")
    void innerPublisherError() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2);
        Spread<Integer, Integer> spread = new Spread<>(upstream,
                i -> {
                    if (i == 2) {
                        return ZeroPublisher.fromFailure(new IOException("inner boom"));
                    }
                    return ZeroPublisher.fromItems(i);
                }, 1, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertFailedWith(IOException.class, "inner boom");
    }

    @Test
    @DisplayName("Upstream error cancels inners and propagates")
    void upstreamError() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromFailure(new IOException("upstream boom"));
        Spread<Integer, Integer> spread = new Spread<>(upstream, ZeroPublisher::fromItems, 1, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertFailedWith(IOException.class, "upstream boom");
    }

    // ---- Cancellation ---- //

    @Test
    @DisplayName("Cancel propagates to upstream")
    void cancelPropagates() {
        AtomicBoolean upstreamCancelled = new AtomicBoolean();
        Flow.Publisher<Integer> upstream = subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
            @Override
            public void request(long n) {
                // no-op
            }

            @Override
            public void cancel() {
                upstreamCancelled.set(true);
            }
        });

        Spread<Integer, Integer> spread = new Spread<>(upstream,
                ZeroPublisher::fromItems, 1, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);
        sub.cancel();

        assertTrue(upstreamCancelled.get());
    }

    @RepeatedTest(1000)
    @DisplayName("Concurrent cancel vs request does not throw")
    void concurrentCancelVsRequest() throws InterruptedException {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2, 3, 4, 5);
        Spread<Integer, Integer> spread = new Spread<>(upstream,
                i -> ZeroPublisher.fromItems(i, i * 10), 2, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        spread.subscribe(sub);

        CountDownLatch latch = new CountDownLatch(2);
        Thread t1 = new Thread(() -> {
            sub.request(10);
            latch.countDown();
        });
        Thread t2 = new Thread(() -> {
            sub.cancel();
            latch.countDown();
        });
        t1.start();
        t2.start();

        assertTrue(latch.await(5, TimeUnit.SECONDS));
        t1.join(5000);
        t2.join(5000);
    }

    // ---- Concurrency limits ---- //

    @Test
    @DisplayName("Concurrency=1 is sequential")
    void concurrencyOneIsSequential() {
        AtomicInteger maxConcurrent = new AtomicInteger();
        AtomicInteger currentConcurrent = new AtomicInteger();

        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2, 3);
        Spread<Integer, Integer> spread = new Spread<>(upstream,
                i -> subscriber -> {
                    int c = currentConcurrent.incrementAndGet();
                    maxConcurrent.updateAndGet(prev -> Math.max(prev, c));
                    subscriber.onSubscribe(new Flow.Subscription() {
                        @Override
                        public void request(long n) {
                            subscriber.onNext(i);
                            currentConcurrent.decrementAndGet();
                            subscriber.onComplete();
                        }

                        @Override
                        public void cancel() {
                        }
                    });
                }, 1, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertCompleted();
        assertEquals(1, maxConcurrent.get(), "Max concurrent inner subscriptions should be 1");
    }

    // ---- Validation ---- //

    @SuppressWarnings("DataFlowIssue")
    @Test
    @DisplayName("Reject null upstream")
    void rejectNullUpstream() {
        assertThrows(NullPointerException.class, () -> new Spread<>(null, i -> ZeroPublisher.empty(), 1, 8));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    @DisplayName("Reject null mapper")
    void rejectNullMapper() {
        assertThrows(NullPointerException.class,
                () -> new Spread<>(ZeroPublisher.empty(), null, 1, 8));
    }

    @Test
    @DisplayName("Reject concurrency <= 0")
    void rejectZeroConcurrency() {
        assertThrows(IllegalArgumentException.class,
                () -> new Spread<>(ZeroPublisher.empty(), i -> ZeroPublisher.empty(), 0, 8));
    }

    @Test
    @DisplayName("Reject prefetch <= 0")
    void rejectZeroPrefetch() {
        assertThrows(IllegalArgumentException.class,
                () -> new Spread<>(ZeroPublisher.empty(), i -> ZeroPublisher.empty(), 1, 0));
    }

    // ---- Additional tests from audit ---- //

    @Test
    @DisplayName("Negative request signals error")
    void negativeRequest() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1);
        Spread<Integer, Integer> spread = new Spread<>(upstream,
                ZeroPublisher::fromItems, 1, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create();
        spread.subscribe(sub);

        sub.request(-1);
        sub.assertFailedWith(IllegalArgumentException.class, "non-positive");
    }

    @Test
    @DisplayName("Prefetch=1 works correctly")
    void prefetchOne() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2);
        Spread<Integer, String> spread = new Spread<>(upstream,
                i -> ZeroPublisher.fromItems(i + "a", i + "b", i + "c"), 1, 1);

        AssertSubscriber<String> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertCompleted().assertItems("1a", "1b", "1c", "2a", "2b", "2c");
    }

    @Test
    @DisplayName("Cancel propagates to inner subscribers")
    void cancelPropagatestoInners() {
        AtomicBoolean innerCancelled = new AtomicBoolean();
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1);
        Spread<Integer, Integer> spread = new Spread<>(upstream,
                i -> subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
                    @Override
                    public void request(long n) {
                        subscriber.onNext(i);
                    }

                    @Override
                    public void cancel() {
                        innerCancelled.set(true);
                    }
                }), 1, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(1);
        spread.subscribe(sub);
        sub.cancel();

        assertTrue(innerCancelled.get());
    }

    @Test
    @DisplayName("Mapper error with concurrency > 1 cancels active inners")
    void mapperErrorWithConcurrency() {
        AtomicBoolean innerCancelled = new AtomicBoolean();
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2);
        Spread<Integer, Integer> spread = new Spread<>(upstream,
                i -> {
                    if (i == 2) {
                        throw new RuntimeException("boom");
                    }
                    return subscriber -> subscriber.onSubscribe(new Flow.Subscription() {
                        @Override
                        public void request(long n) {
                            subscriber.onNext(i);
                        }

                        @Override
                        public void cancel() {
                            innerCancelled.set(true);
                        }
                    });
                }, 4, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertFailedWith(RuntimeException.class, "boom");
        assertTrue(innerCancelled.get());
    }

    @Test
    @DisplayName("Resubscription creates independent streams")
    void resubscription() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1, 2);
        Spread<Integer, Integer> spread = new Spread<>(upstream,
                i -> ZeroPublisher.fromItems(i * 10), 1, 8);

        AssertSubscriber<Integer> sub1 = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub1);
        sub1.assertCompleted().assertItems(10, 20);

        AssertSubscriber<Integer> sub2 = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub2);
        sub2.assertCompleted().assertItems(10, 20);
    }

    @Test
    @DisplayName("Cancel inside onNext does not deliver onComplete")
    void cancelInsideOnNextNoComplete() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(1);
        Spread<Integer, Integer> spread = new Spread<>(upstream, ZeroPublisher::fromItems, 1, 8);

        AtomicBoolean completeCalled = new AtomicBoolean();
        AtomicBoolean errorCalled = new AtomicBoolean();
        Flow.Subscriber<Integer> sub = new Flow.Subscriber<>() {
            Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription s) {
                this.subscription = s;
                s.request(1);
            }

            @Override
            public void onNext(Integer item) {
                subscription.cancel();
            }

            @Override
            public void onError(Throwable throwable) {
                errorCalled.set(true);
            }

            @Override
            public void onComplete() {
                completeCalled.set(true);
            }
        };
        spread.subscribe(sub);

        assertFalse(completeCalled.get(), "onComplete must not be called after cancel");
        assertFalse(errorCalled.get(), "onError must not be called after cancel");
    }

    @Test
    @DisplayName("Single item upstream with single item inner")
    void singleItemSingleInner() {
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(42);
        Spread<Integer, String> spread = new Spread<>(upstream,
                i -> ZeroPublisher.fromItems("val-" + i), 1, 8);

        AssertSubscriber<String> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertCompleted().assertItems("val-42");
    }

    @Test
    @DisplayName("Large number of items with concurrency=1")
    void manyItemsConcatMap() {
        int count = 100;
        Integer[] items = new Integer[count];
        for (int i = 0; i < count; i++) {
            items[i] = i;
        }
        Flow.Publisher<Integer> upstream = ZeroPublisher.fromItems(items);
        Spread<Integer, Integer> spread = new Spread<>(upstream,
                i -> ZeroPublisher.fromItems(i, i + 1000), 1, 8);

        AssertSubscriber<Integer> sub = AssertSubscriber.create(Long.MAX_VALUE);
        spread.subscribe(sub);

        sub.assertCompleted();
        assertEquals(200, sub.getItems().size());
        assertEquals(0, sub.getItems().get(0));
        assertEquals(1000, sub.getItems().get(1));
        assertEquals(1, sub.getItems().get(2));
        assertEquals(1001, sub.getItems().get(3));
    }
}
