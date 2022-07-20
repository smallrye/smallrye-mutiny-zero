package mutiny.zero.tck;

import java.util.concurrent.Flow.Publisher;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import mutiny.zero.ZeroPublisher;

public class EmptyPublisherTckTest extends FlowPublisherVerification<Long> {

    public EmptyPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return ZeroPublisher.empty();
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 0L;
    }
}
