package mutiny.zero.internal;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

public class IterablePublisher<T> implements Publisher<T> {

    private final Iterable<T> iterable;

    public IterablePublisher(Iterable<T> iterable) {
        this.iterable = iterable;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        Iterator<T> iterator = iterable.iterator();
        if (!iterator.hasNext()) {
            subscriber.onSubscribe(new AlreadyCompletedSubscription());
            subscriber.onComplete();
        } else {
            subscriber.onSubscribe(new IteratorSubscription<>(iterator, subscriber));
        }
    }

}
