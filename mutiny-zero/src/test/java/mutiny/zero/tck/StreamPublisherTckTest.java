package mutiny.zero.tck;

import java.util.concurrent.Flow.Publisher;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import mutiny.zero.ZeroPublisher;

public class StreamPublisherTckTest extends FlowPublisherVerification<Long> {

    public StreamPublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        Supplier<Stream<Long>> supplier = () -> LongStream.rangeClosed(1, elements).boxed();
        return ZeroPublisher.fromStream(supplier);
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
