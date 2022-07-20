package mutiny.zero.tck;

import java.io.IOException;
import java.util.concurrent.Flow.Publisher;
import java.util.stream.LongStream;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import mutiny.zero.ZeroPublisher;

public class FailurePublisherTckTest extends FlowPublisherVerification<Long> {

    public FailurePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        // This is irrelevant so we just pass some unrelated publisher
        Long[] list = LongStream.rangeClosed(1, elements).boxed().toArray(Long[]::new);
        return ZeroPublisher.fromItems(list);
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return ZeroPublisher.fromFailure(new IOException("boom"));
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024L;
    }
}
