package mutiny.zero.vertxpublishers.tck;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.streams.ReadStream;
import mutiny.zero.vertxpublishers.VertxPublisher;

public class VertxPublisherTckTest extends FlowPublisherVerification<Long> {

    public VertxPublisherTckTest() {
        super(new TestEnvironment());
    }

    Vertx vertx;

    @BeforeMethod
    @Override
    public void setUp() throws Exception {
        super.setUp();
        vertx = Vertx.vertx();
    }

    @AfterMethod
    public void tearDown() {
        AtomicBoolean closed = new AtomicBoolean();
        vertx.close().onSuccess(ar -> closed.set(true));
        await().atMost(5, TimeUnit.SECONDS).untilTrue(closed);
    }

    static final class PeriodicStream implements ReadStream<Long> {

        final Vertx vertx;
        final long maxElements;

        Handler<Long> handler;
        Handler<Void> endHandler;

        final AtomicLong demand = new AtomicLong();
        final AtomicLong counter = new AtomicLong();
        volatile boolean active = true;

        public PeriodicStream(Vertx vertx, long period, long maxElements) {
            this.vertx = vertx;
            this.maxElements = maxElements;
            vertx.setPeriodic(period, this::tick);
        }

        private void tick(Long tick) {
            if (handler == null || !active) {
                return;
            }
            if (demand.get() > 0L) {
                handler.handle(tick);
                demand.decrementAndGet();
                if (counter.incrementAndGet() >= maxElements) {
                    endHandler.handle(null);
                    vertx.cancelTimer(tick);
                }
            }
        }

        @Override
        public ReadStream<Long> exceptionHandler(Handler<Throwable> handler) {
            return this;
        }

        @Override
        public ReadStream<Long> handler(Handler<Long> handler) {
            this.handler = handler;
            return this;
        }

        @Override
        public ReadStream<Long> pause() {
            this.active = false;
            return this;
        }

        @Override
        public ReadStream<Long> resume() {
            this.active = true;
            return this;
        }

        @Override
        public ReadStream<Long> fetch(long amount) {
            this.active = true;
            if (demand.addAndGet(amount) < 0L) {
                demand.set(Long.MAX_VALUE);
            }
            return this;
        }

        @Override
        public ReadStream<Long> endHandler(Handler<Void> endHandler) {
            this.endHandler = endHandler;
            return this;
        }
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        return VertxPublisher.fromSupplier(() -> new PeriodicStream(vertx, 25, elements));
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return null;
    }
}
