package mutiny.zero.operators.tck;

import java.util.Random;
import java.util.concurrent.Flow;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import io.smallrye.mutiny.Multi;
import mutiny.zero.operators.Transform;

public class TransformTckTest extends FlowPublisherVerification<Long> {

    public TransformTckTest() {
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
        return new Transform<>(source, Math::abs);
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return null;
    }
}
