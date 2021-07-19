package mutiny.zero.tck;

import org.reactivestreams.Publisher;
import org.reactivestreams.tck.TestEnvironment;
import org.reactivestreams.tck.junit5.PublisherVerification;

import mutiny.zero.BackpressureStrategy;
import mutiny.zero.TubeConfiguration;
import mutiny.zero.ZeroPublisher;

public class IgnoringTubePublisherTckTest extends PublisherVerification<Long> {

    public IgnoringTubePublisherTckTest() {
        super(new TestEnvironment());
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        TubeConfiguration configuration = new TubeConfiguration().withBackpressureStrategy(BackpressureStrategy.IGNORE);
        return ZeroPublisher.create(configuration, tube -> TubeEmitLoop.loop(tube, elements));
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }
}
