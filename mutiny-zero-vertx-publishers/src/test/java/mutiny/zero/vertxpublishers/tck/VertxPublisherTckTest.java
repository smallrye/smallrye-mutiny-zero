package mutiny.zero.vertxpublishers.tck;

import static org.awaitility.Awaitility.await;

import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import io.smallrye.mutiny.Multi;
import io.vertx.core.Vertx;
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
        vertx.close(ar -> closed.set(true));
        await().atMost(5, TimeUnit.SECONDS).untilTrue(closed);
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        // Wrapping in a Multi to limit to the TCK-requested number of elements
        return Multi.createFrom().publisher(VertxPublisher.fromSupplier(() -> vertx.periodicStream(25)))
                .select().first(elements);
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return null;
    }
}
