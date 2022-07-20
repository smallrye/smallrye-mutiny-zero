package mutiny.zero.tck;

import java.util.concurrent.Flow.Publisher;
import java.util.stream.LongStream;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import mutiny.zero.PublisherHelpers;
import mutiny.zero.ZeroPublisher;

public class MapHelperPublisherTckTest extends FlowPublisherVerification<Long> {

    public MapHelperPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        Long[] list = LongStream.rangeClosed(1, elements).boxed().toArray(Long[]::new);
        return PublisherHelpers.map(ZeroPublisher.fromItems(list), n -> n / 2L);
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return null;
    }

    @Override
    public long maxElementsFromPublisher() {
        return 1024L;
    }
}
