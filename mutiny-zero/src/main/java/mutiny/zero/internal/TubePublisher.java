package mutiny.zero.internal;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.function.Consumer;

import mutiny.zero.BackpressureStrategy;
import mutiny.zero.Tube;
import mutiny.zero.TubeConfiguration;

public class TubePublisher<T> implements Publisher<T> {

    private final BackpressureStrategy backpressureStrategy;
    private final int bufferSize;
    private final Consumer<Tube<T>> tubeConsumer;

    public TubePublisher(TubeConfiguration configuration, Consumer<Tube<T>> tubeConsumer) {
        this.backpressureStrategy = configuration.getBackpressureStrategy();
        this.bufferSize = configuration.getBufferSize();
        this.tubeConsumer = tubeConsumer;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        TubeBase<T> tube = switch (backpressureStrategy) {
            case BUFFER -> new BufferingTube<>(subscriber, bufferSize);
            case UNBOUNDED_BUFFER -> new UnbounbedBufferingTube<>(subscriber);
            case DROP -> new DroppingTube<>(subscriber);
            case ERROR -> new ErroringTube<>(subscriber);
            case IGNORE -> new IgnoringTube<>(subscriber);
            case LATEST -> new LatestTube<>(subscriber, bufferSize);
        };
        subscriber.onSubscribe(tube);
        tubeConsumer.accept(tube);
    }
}
