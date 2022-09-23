package mutiny.zero.vertxpublishers;

import java.util.concurrent.Flow;

import io.vertx.core.streams.ReadStream;
import mutiny.zero.BackpressureStrategy;
import mutiny.zero.TubeConfiguration;
import mutiny.zero.ZeroPublisher;

abstract class PublisherBase<T> implements Flow.Publisher<T> {

    protected void adapt(Flow.Subscriber<? super T> subscriber, ReadStream<T> stream) {
        TubeConfiguration conf = new TubeConfiguration().withBackpressureStrategy(BackpressureStrategy.ERROR);
        Flow.Publisher<T> publisher = ZeroPublisher.create(conf, tube -> {
            stream.pause();
            stream.handler(tube::send);
            stream.exceptionHandler(tube::fail);
            stream.endHandler(v -> tube.complete());
            tube.whenCancelled(() -> {
                stream.pause();
                stream.handler(null);
                stream.exceptionHandler(null);
                stream.endHandler(null);
            });
            tube.whenRequested(stream::fetch);
        });
        publisher.subscribe(subscriber);
    }
}
