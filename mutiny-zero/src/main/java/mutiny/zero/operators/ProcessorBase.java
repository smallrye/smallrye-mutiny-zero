package mutiny.zero.operators;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;

import org.jspecify.annotations.Nullable;

abstract class ProcessorBase<I, O> implements Flow.Processor<I, O>, Flow.Subscription {

    private Flow.@Nullable Subscriber<? super O> downstream;
    private Flow.@Nullable Subscription upstreamSubscription;

    private final AtomicBoolean cancelled = new AtomicBoolean();

    protected boolean cancelled() {
        return cancelled.get();
    }

    protected Flow.Subscription upstreamSubscription() {
        assert upstreamSubscription != null;
        return upstreamSubscription;
    }

    protected Flow.Subscriber<? super O> downstream() {
        assert downstream != null;
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
        assert downstream != null;
        downstream.onSubscribe(this);
    }

    @Override
    public void onError(Throwable throwable) {
        if (!cancelled()) {
            cancel();
            assert downstream != null;
            downstream.onError(throwable);
        }
    }

    @Override
    public void onComplete() {
        if (!cancelled()) {
            assert downstream != null;
            downstream.onComplete();
        }
    }

    // ---- Subscription

    @Override
    public void request(long n) {
        if (!cancelled()) {
            assert upstreamSubscription != null;
            upstreamSubscription.request(n);
        }
    }

    @Override
    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            assert upstreamSubscription != null;
            upstreamSubscription.cancel();
            upstreamSubscription = null;
        }
    }
}
