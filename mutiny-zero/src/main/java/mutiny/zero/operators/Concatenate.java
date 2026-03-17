package mutiny.zero.operators;

import static java.util.Objects.requireNonNull;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.jspecify.annotations.Nullable;

import mutiny.zero.internal.Helper;

/**
 * A {@link java.util.concurrent.Flow.Publisher} that is the concatenation of several ones.
 * <p>
 * The first publisher in the list is the first to be subscribed.
 * Once it completes the next one is subscribed and so on, up to the completion of the last one.
 * <p>
 * If any publisher sends an error signal then the concatenation stream ends with that error.
 *
 * @param <T> the elements type
 */
public class Concatenate<T> implements Flow.Publisher<T> {

    private final List<Flow.Publisher<T>> publishers;

    /**
     * Create a new concatenation publisher.
     *
     * @param publishers the list of publishers, must not be {@code null}, must not contain {@code null}
     */
    public Concatenate(List<Flow.Publisher<T>> publishers) {
        this.publishers = requireNonNull(publishers, "The publishers list cannot be null");
        for (Flow.Publisher<T> publisher : publishers) {
            requireNonNull(publisher, "A publisher cannot be null");
        }
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        Processor processor = new Processor();
        processor.subscribe(subscriber);
    }

    private class Processor implements Flow.Processor<T, T>, Flow.Subscription {
        private Flow.@Nullable Subscriber<? super T> downstream;
        private Flow.@Nullable Subscription upstreamSubscription;

        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicLong demand = new AtomicLong();
        private volatile boolean unboundedDemand;
        private boolean downstreamIsReady;
        private final Iterator<Flow.Publisher<T>> publisherIterator = publishers.iterator();

        @Override
        public void subscribe(Flow.Subscriber<? super T> subscriber) {
            downstream = subscriber;
            subscribeNext();
        }

        private void subscribeNext() {
            if (publisherIterator.hasNext()) {
                Flow.Publisher<T> publisher = publisherIterator.next();
                publisher.subscribe(this);
            } else {
                assert downstream != null;
                downstream.onComplete();
            }
        }

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (cancelled.get()) {
                return;
            }
            this.upstreamSubscription = subscription;
            if (downstreamIsReady) {
                long n = demand.get();
                if (n > 0L) {
                    this.upstreamSubscription.request(n);
                }
            } else {
                downstreamIsReady = true;
                assert downstream != null;
                downstream.onSubscribe(this);
            }
        }

        @Override
        public void onNext(T item) {
            if (!cancelled.get()) {
                if (!unboundedDemand) {
                    demand.decrementAndGet();
                }
                assert downstream != null;
                downstream.onNext(item);
            }
        }

        @Override
        public void onError(Throwable throwable) {
            if (!cancelled.get()) {
                cancel();
                assert downstream != null;
                downstream.onError(throwable);
            }
        }

        @Override
        public void onComplete() {
            if (!cancelled.get()) {
                subscribeNext();
            }
        }

        @Override
        public void request(long n) {
            if (cancelled.get()) {
                return;
            }
            if (n <= 0L) {
                onError(Helper.negativeRequest(n));
            } else {
                Helper.add(demand, n);
                if (n == Long.MAX_VALUE) {
                    unboundedDemand = true;
                }
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
}
