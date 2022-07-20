package mutiny.zero.tck;

import java.util.concurrent.Flow;

import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.flow.FlowPublisherVerification;

import mutiny.zero.BackpressureStrategy;
import mutiny.zero.TubeConfiguration;
import mutiny.zero.ZeroPublisher;

public class UnboundedBufferingTubePublisherTckTest extends FlowPublisherVerification<Long> {

    public UnboundedBufferingTubePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Flow.Publisher<Long> createFlowPublisher(long elements) {
        TubeConfiguration configuration = new TubeConfiguration()
                .withBackpressureStrategy(BackpressureStrategy.UNBOUNDED_BUFFER);
        return ZeroPublisher.create(configuration, tube -> TubeEmitLoop.loop(tube, elements));
    }

    @Override
    public Flow.Publisher<Long> createFailedFlowPublisher() {
        return null;
    }
}
