package mutiny.zero.tck;

import java.util.Iterator;
import java.util.concurrent.Flow.Publisher;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import mutiny.zero.ZeroPublisher;

public class GeneratorPublisherTckTest extends FlowPublisherVerification<Long> {

    public GeneratorPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        return ZeroPublisher.fromGenerator(() -> 10L, init -> new Iterator<Long>() {

            long current = init;
            long emitted = 0L;

            @Override
            public boolean hasNext() {
                return emitted < elements;
            }

            @Override
            public Long next() {
                emitted++;
                return current++;
            }
        });
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return null;
    }
}
