package mutiny.zero.tck;

import java.util.concurrent.Flow;
import java.util.stream.LongStream;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import mutiny.zero.ZeroPublisher;

public class IterablePublisherTckTest extends FlowPublisherVerification<Long> {

    public IterablePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        Long[] list = LongStream.rangeClosed(1, elements).boxed().toArray(Long[]::new);
        return ZeroPublisher.fromItems(list);
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024L;
    }
}
