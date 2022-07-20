package mutiny.zero.internal;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;

public class EmptyPublisher<T> implements Publisher<T> {

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        subscriber.onSubscribe(new AlreadyCompletedSubscription());
        subscriber.onComplete();
    }
}
