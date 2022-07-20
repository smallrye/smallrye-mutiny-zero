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
        TubeBase<T> tube = null;
        switch (backpressureStrategy) {
            case BUFFER:
                tube = new BufferingTube<>(subscriber, bufferSize);
                break;
            case UNBOUNDED_BUFFER:
                tube = new UnbounbedBufferingTube<>(subscriber);
                break;
            case DROP:
                tube = new DroppingTube<>(subscriber);
                break;
            case ERROR:
                tube = new ErroringTube<>(subscriber);
                break;
            case IGNORE:
                tube = new IgnoringTube<>(subscriber);
                break;
            case LATEST:
                tube = new LatestTube<>(subscriber, bufferSize);
                break;
        }
        subscriber.onSubscribe(tube);
        tubeConsumer.accept(tube);
    }
}
