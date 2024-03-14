package mutiny.zero.tck;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import mutiny.zero.ZeroPublisher;

public class CompletionStageTckPublisherTest extends FlowPublisherVerification<Long> {

    public CompletionStageTckPublisherTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return ZeroPublisher.fromCompletionStage(() -> CompletableFuture.supplyAsync(() -> 69L));
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1L;
    }
}
