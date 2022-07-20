package mutiny.zero.internal;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;

public class FailurePublisher<T> implements Publisher<T> {

    private final Throwable failure;

    public FailurePublisher(Throwable failure) {
        this.failure = failure;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        subscriber.onSubscribe(new AlreadyCompletedSubscription());
        subscriber.onError(failure);
    }
}
