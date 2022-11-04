package mutiny.zero.operators.tck;

import java.util.List;
import java.util.Random;
import java.util.concurrent.Flow;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import io.smallrye.mutiny.Multi;
import mutiny.zero.ZeroPublisher;
import mutiny.zero.operators.Concatenate;

public class ConcatenateTckTest extends FlowPublisherVerification<Long> {

    public ConcatenateTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long count) {
        Flow.Publisher<Long> source;
        if (count > 0) {
            Random random = new Random();
            source = Multi.createBy().repeating().supplier(random::nextLong).atMost(count);
        } else {
            source = Multi.createFrom().empty();
        }
        return new Concatenate<>(List.of(ZeroPublisher.empty(), ZeroPublisher.empty(), source));
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return null;
    }
}
