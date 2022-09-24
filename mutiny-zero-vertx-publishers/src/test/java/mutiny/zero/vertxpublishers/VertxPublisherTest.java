package mutiny.zero.vertxpublishers;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.*;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;

@DisplayName("Vert.x publisher tests")
class VertxPublisherTest {

    @Nested
    @DisplayName("Regular tests")
    class RegularTests {

        Vertx vertx;

        @BeforeEach
        void prepare() {
            vertx = Vertx.vertx();
        }

        @AfterEach
        void cleanup() {
            AtomicBoolean closed = new AtomicBoolean();
            vertx.close(ar -> closed.set(true));
            await().atMost(5, TimeUnit.SECONDS).untilTrue(closed);
        }

        @Test
        @DisplayName("Test using a periodic stream")
        void periodicStream() throws InterruptedException {
            Flow.Publisher<Long> publisher = VertxPublisher.fromSupplier(() -> vertx.periodicStream(50));

            AssertSubscriber<String> sub = AssertSubscriber.create();
            Multi.createFrom().publisher(publisher)
                    .onItem().transform(ignored -> "tick")
                    .select().first(10)
                    .subscribe().withSubscriber(sub);

            Thread.sleep(250);
            sub.assertHasNotReceivedAnyItem();
            sub.request(2);
            Thread.sleep(500);
            sub.assertItems("tick", "tick");
            sub.request(1);
            Thread.sleep(500);
            sub.assertItems("tick", "tick", "tick");
            sub.cancel();
        }

        @Test
        @DisplayName("Test using a TCP stream")
        void netServer() throws InterruptedException {
            AtomicBoolean ready = new AtomicBoolean();
            AtomicInteger port = new AtomicInteger();

            vertx.createNetServer().connectHandler(socket -> {
                AtomicLong counter = new AtomicLong();
                vertx.setPeriodic(100, tick -> {
                    socket.write("[ tick :: " + counter.getAndIncrement() + " ]\n");
                });
            }).listen(0).onSuccess(server -> {
                port.set(server.actualPort());
                ready.set(true);
            });

            await().atMost(5, TimeUnit.SECONDS).untilTrue(ready);

            Flow.Publisher<Buffer> publisher = VertxPublisher
                    .fromFuture(() -> vertx.createNetClient().connect(port.get(), "localhost"));

            AssertSubscriber<String> sub = AssertSubscriber.create();
            Multi.createFrom().publisher(publisher)
                    .onItem().transform(buffer -> buffer.toString(StandardCharsets.UTF_8))
                    .subscribe().withSubscriber(sub);

            Thread.sleep(1000);
            sub.assertHasNotReceivedAnyItem();
            sub.request(10);
            Thread.sleep(1000);

            List<String> items = sub.getItems();
            int firstBatchCount = items.size();
            assertTrue(firstBatchCount > 2);
            assertEquals("[ tick :: 0 ]\n", items.get(0));
            assertEquals("[ tick :: 1 ]\n", items.get(1));

            Thread.sleep(250);
            assertEquals(firstBatchCount, sub.getItems().size());

            sub.request(1);
            Thread.sleep(500);
            assertEquals(firstBatchCount + 1, sub.getItems().size());

            sub.cancel();
        }
    }

    @Nested
    @DisplayName("Null and throwable cases")
    class NullsAndThrowables {

        @Test
        @DisplayName("Reject a null supplier")
        void rejectNullSupplier() {
            NullPointerException exception = assertThrows(NullPointerException.class, () -> VertxPublisher.fromSupplier(null));
            assertEquals("The stream supplier cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Reject a null future supplier")
        void rejectNullFutureSupplier() {
            NullPointerException exception = assertThrows(NullPointerException.class, () -> VertxPublisher.fromFuture(null));
            assertEquals("The future supplier cannot be null", exception.getMessage());
        }

        @Test
        @DisplayName("Handle null returning supplier")
        void handleNullReturningSupplier() {
            AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
            VertxPublisher.fromSupplier(() -> null).subscribe(sub);
            sub.assertFailedWith(NullPointerException.class, "The stream cannot be null");
        }

        @Test
        @DisplayName("Handle null returning future supplier")
        void handleNullReturningFutureSupplier() {
            AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
            VertxPublisher.fromFuture(() -> null).subscribe(sub);
            sub.assertFailedWith(NullPointerException.class, "The future cannot be null");
        }

        @Test
        @DisplayName("Handle failed futures")
        void handleFailedFutures() {
            AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
            VertxPublisher.fromFuture(() -> Future.failedFuture(new RuntimeException("Yolo"))).subscribe(sub);
            sub.assertFailedWith(RuntimeException.class, "Yolo");
        }

        @Test
        @DisplayName("Handle exception throwing suppliers")
        void handleThrowingSuppliers() {
            AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
            VertxPublisher.fromSupplier(() -> {
                throw new RuntimeException("Yolo");
            }).subscribe(sub);
            sub.assertFailedWith(RuntimeException.class, "Yolo");
        }

        @Test
        @DisplayName("Handle exception throwing future suppliers")
        void handleThrowingFutureSuppliers() {
            AssertSubscriber<Object> sub = AssertSubscriber.create(Long.MAX_VALUE);
            VertxPublisher.fromFuture(() -> {
                throw new RuntimeException("Yolo");
            }).subscribe(sub);
            sub.assertFailedWith(RuntimeException.class, "Yolo");
        }
    }
}
