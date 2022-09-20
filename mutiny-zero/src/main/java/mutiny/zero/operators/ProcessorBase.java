package mutiny.zero.operators;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

abstract class ProcessorBase<I, O> implements Flow.Processor<I, O>, Flow.Subscription {

    private Flow.Subscriber<? super O> downstream;
    private Flow.Subscription upstreamSubscription;

    private final AtomicBoolean cancelled = new AtomicBoolean();

    protected boolean cancelled() {
        return cancelled.get();
    }

    protected Flow.Subscriber<? super O> downstream() {
        return downstream;
    }

    // ---- Publisher

    @Override
    public void subscribe(Flow.Subscriber<? super O> subscriber) {
        downstream = subscriber;
    }

    // ---- Subscriber

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        this.upstreamSubscription = subscription;
        downstream.onSubscribe(this);
    }

    @Override
    public void onError(Throwable throwable) {
        if (!cancelled()) {
            cancel();
            downstream.onError(throwable);
        }
    }

    @Override
    public void onComplete() {
        if (!cancelled()) {
            downstream.onComplete();
        }
    }

    // ---- Subscription

    @Override
    public void request(long n) {
        if (!cancelled()) {
            upstreamSubscription.request(n);
        }
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            upstreamSubscription.cancel();
            upstreamSubscription = null;
        }
    }
}
