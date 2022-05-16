package mutiny.zero.flow.adapters.tors;

import java.util.concurrent.Flow;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.common.Wrapper;

public class SubscriberAdapterFromFlow<T> implements Subscriber<T>, Wrapper<Flow.Subscriber<T>> {

    private final Flow.Subscriber<T> subscriber;

    public SubscriberAdapterFromFlow(Flow.Subscriber<T> subscriber) {
        this.subscriber = subscriber;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        subscriber.onSubscribe(AdaptersToFlow.subscription(subscription));
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
    public Flow.Subscriber<T> unwrap() {
        return subscriber;
    }
}
