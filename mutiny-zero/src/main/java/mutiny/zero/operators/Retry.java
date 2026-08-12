package mutiny.zero.operators;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import mutiny.zero.internal.Helper;

/**
 * A {@link Flow.Publisher} that retries on failure by re-subscribing to its upstream.
 * A {@link Predicate} controls when to retry, and when to stop retrying.
 * <p>
 * <p>
 * Note: this retry operator does not perform advanced time-based re-subscriptions (e.g., exponential back-off).
 *
 * @param <T> the elements type
 */
public class Retry<T> implements Flow.Publisher<T> {

    private final Flow.Publisher<T> upstream;
    private final Predicate<Throwable> retryPredicate;

    public static Predicate<Throwable> always() {
        return (err) -> true;
    }

    public static Predicate<Throwable> atMost(int count) {
        if (count <= 0) {
            throw new IllegalArgumentException("count must be strictly positive");
        }
        return new Predicate<>() {
            int n;

            @Override
            public boolean test(Throwable throwable) {
                if (n == count) {
                    return false;
                } else {
                    n = n + 1;
                    return true;
                }
            }
        };
    }

    /**
     * Build a new retry publisher.
     *
     * @param upstream the upstream publisher
     * @param retryPredicate the retry predicate
     */
    public Retry(Flow.Publisher<T> upstream, Predicate<Throwable> retryPredicate) {
        this.upstream = requireNonNull(upstream, "The upstream cannot be null");
        this.retryPredicate = requireNonNull(retryPredicate, "The retry predicate cannot be null");
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        Processor processor = new Processor();
        processor.subscribe(subscriber);
        upstream.subscribe(processor);
    }

    private class Processor extends ProcessorBase<T, T> {

        private boolean subscribedDownstream;
        private final AtomicLong pendingDemand = new AtomicLong();

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            if (!subscribedDownstream) {
                subscribedDownstream = true;
                super.onSubscribe(subscription);
            } else {
                // Retry: update upstream subscription without re-signaling downstream
                setUpstreamSubscription(subscription);
                long demand = pendingDemand.get();
                if (demand > 0L) {
                    subscription.request(demand);
                }
            }
        }

        @Override
        public void request(long n) {
            Helper.add(pendingDemand, n);
            super.request(n);
        }

        @Override
        public void onNext(T item) {
            if (!cancelled()) {
                pendingDemand.decrementAndGet();
                downstream().onNext(item);
            }
        }

        @Override
        public void onError(Throwable err) {
            if (!cancelled()) {
                boolean retry;
                try {
                    retry = retryPredicate.test(err);
                } catch (Throwable t) {
                    cancel();
                    downstream().onError(t);
                    return;
                }
                if (retry) {
                    upstream.subscribe(this);
                } else {
                    cancel();
                    downstream().onError(err);
                }
            }
        }
    }
}
