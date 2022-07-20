package mutiny.zero.tck;

import java.util.concurrent.Flow.Publisher;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import mutiny.zero.BackpressureStrategy;
import mutiny.zero.TubeConfiguration;
import mutiny.zero.ZeroPublisher;

public class ErrorTubePublisherTckTest extends FlowPublisherVerification<Long> {

    public ErrorTubePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createFlowPublisher(long elements) {
        TubeConfiguration configuration = new TubeConfiguration().withBackpressureStrategy(BackpressureStrategy.ERROR);
        return ZeroPublisher.create(configuration, tube -> TubeEmitLoop.loop(tube, elements));
    }

    @Override
    public Publisher<Long> createFailedFlowPublisher() {
        return null;
    }
}
