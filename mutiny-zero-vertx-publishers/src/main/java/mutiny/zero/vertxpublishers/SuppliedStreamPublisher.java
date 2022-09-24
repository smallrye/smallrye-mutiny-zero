package mutiny.zero.vertxpublishers;

import java.util.concurrent.Flow;
import java.util.function.Supplier;

import io.vertx.core.streams.ReadStream;

class SuppliedStreamPublisher<T> extends PublisherBase<T> {

    private final Supplier<ReadStream<T>> streamSupplier;

    SuppliedStreamPublisher(Supplier<ReadStream<T>> streamSupplier) {
        this.streamSupplier = streamSupplier;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        ReadStream<T> stream;
        try {
            stream = streamSupplier.get();
        } catch (Throwable err) {
            subscriber.onSubscribe(new NoopSubscription());
            subscriber.onError(err);
            return;
        }
        if (stream == null) {
            subscriber.onSubscribe(new NoopSubscription());
            subscriber.onError(new NullPointerException("The stream cannot be null"));
        } else {
            adapt(subscriber, stream);
        }
    }
}
