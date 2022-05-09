package mutiny.zero.flow.adapters.toflow;

import java.util.concurrent.Flow;

import org.reactivestreams.Subscriber;

import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;
import mutiny.zero.flow.adapters.common.Wrapper;

public class SubscriberAdapterFromRs<T> implements Flow.Subscriber<T>, Wrapper<Subscriber<T>> {

    private final Subscriber<T> subscriber;

    public SubscriberAdapterFromRs(Subscriber<T> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        subscriber.onSubscribe(AdaptersToReactiveStreams.subscription(subscription));
    }

    @Override
    public void onNext(T item) {
        subscriber.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        subscriber.onError(throwable);
    }

    @Override
    public void onComplete() {
        subscriber.onComplete();
    }

    @Override
    public Subscriber<T> unwrap() {
        return subscriber;
    }
}
