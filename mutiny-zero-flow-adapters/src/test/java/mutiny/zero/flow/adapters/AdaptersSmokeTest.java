package mutiny.zero.flow.adapters;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

class AdaptersSmokeTest {

    @Test
    void rejectNulls() {
        assertThrows(NullPointerException.class, () -> AdaptersToFlow.publisher(null));
        assertThrows(NullPointerException.class, () -> AdaptersToFlow.subscriber(null));
        assertThrows(NullPointerException.class, () -> AdaptersToFlow.processor(null));
        assertThrows(NullPointerException.class, () -> AdaptersToFlow.subscription(null));

        assertThrows(NullPointerException.class, () -> AdaptersToReactiveStreams.publisher(null));
        assertThrows(NullPointerException.class, () -> AdaptersToReactiveStreams.subscriber(null));
        assertThrows(NullPointerException.class, () -> AdaptersToReactiveStreams.processor(null));
        assertThrows(NullPointerException.class, () -> AdaptersToReactiveStreams.subscription(null));
    }

    @Test
    void flowToRs() throws InterruptedException {
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>();

        CountDownLatch latch = new CountDownLatch(1);
        List<String> items = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean();
        AtomicReference<Throwable> failure = new AtomicReference<>();

        Publisher<String> rsPublisher = AdaptersToReactiveStreams.publisher(publisher);
        rsPublisher.subscribe(new Subscriber<>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(Long.MAX_VALUE);
                // Dirty, but works
                publisher.submit("foo");
                publisher.submit("bar");
                publisher.submit("baz");
                publisher.close();
            }

            @Override
            public void onNext(String s) {
                items.add(s);
            }

            @Override
            public void onError(Throwable t) {
                failure.set(t);
                latch.countDown();
            }

            @Override
            public void onComplete() {
                completed.set(true);
                latch.countDown();
            }
        });

        assertTrue(latch.await(5, TimeUnit.SECONDS));

        assertTrue(completed.get());
        assertNull(failure.get());
        assertEquals(3, items.size());
        assertEquals("foo", items.get(0));
        assertEquals("bar", items.get(1));
        assertEquals("baz", items.get(2));
    }

    @Test
    void avoidExcessiveWrapping() {
        Publisher<Object> initial = new Publisher<>() {
            @Override
            public void subscribe(Subscriber<? super Object> s) {
                // Nothing
            }
        };
        Flow.Publisher<Object> flowPublisher = AdaptersToFlow.publisher(initial);
        Publisher<Object> rsPublisher = AdaptersToReactiveStreams.publisher(flowPublisher);

        assertSame(initial, rsPublisher);
    }
}
