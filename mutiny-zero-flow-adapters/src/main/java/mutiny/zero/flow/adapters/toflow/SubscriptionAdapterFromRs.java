package mutiny.zero.flow.adapters.toflow;

import java.util.concurrent.Flow;

import org.reactivestreams.Subscription;

import mutiny.zero.flow.adapters.common.Wrapper;

public class SubscriptionAdapterFromRs implements Flow.Subscription, Wrapper<Subscription> {

    private final Subscription subscription;

    public SubscriptionAdapterFromRs(Subscription subscription) {
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
    public Subscription unwrap() {
        return subscription;
    }
}
