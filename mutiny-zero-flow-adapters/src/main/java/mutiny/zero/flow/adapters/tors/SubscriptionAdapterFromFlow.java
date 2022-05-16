package mutiny.zero.flow.adapters.tors;

import java.util.concurrent.Flow;

import org.reactivestreams.Subscription;

import mutiny.zero.flow.adapters.common.Wrapper;

public class SubscriptionAdapterFromFlow implements Subscription, Wrapper<Flow.Subscription> {

    private final Flow.Subscription subscription;

    public SubscriptionAdapterFromFlow(Flow.Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void request(long n) {
        subscription.request(n);
    }

    @Override
    public void cancel() {
        subscription.cancel();
    }

    @Override
    public Flow.Subscription unwrap() {
        return subscription;
    }
}
