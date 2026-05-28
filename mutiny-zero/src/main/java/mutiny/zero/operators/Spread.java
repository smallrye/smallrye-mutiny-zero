package mutiny.zero.operators;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

import mutiny.zero.internal.Helper;

/**
 * A {@link Flow.Publisher} that maps each upstream item to a {@link Flow.Publisher} and flattens the results
 * with bounded concurrency.
 * <p>
 * With {@code concurrency = 1} this behaves as a concatMap (strict ordering).
 * With {@code concurrency > 1} items from different inner publishers may interleave.
 *
 * @param <T> the upstream item type
 * @param <R> the downstream (flattened) item type
 */
public class Spread<T, R> implements Flow.Publisher<R> {

    private final Flow.Publisher<T> upstream;
    private final Function<T, Flow.@Nullable Publisher<R>> mapper;
    private final int concurrency;
    private final int prefetch;

    /**
     * Create a new spread (flatMap) publisher.
     *
     * @param upstream the upstream publisher, must not be {@code null}
     * @param mapper the mapping function, must not be {@code null}
     * @param concurrency the maximum number of concurrent inner subscriptions, must be positive
     * @param prefetch the per-inner request batch size, must be positive
     */
    public Spread(Flow.Publisher<T> upstream, Function<T, Flow.@Nullable Publisher<R>> mapper, int concurrency, int prefetch) {
        this.upstream = requireNonNull(upstream, "The upstream publisher cannot be null");
        this.mapper = requireNonNull(mapper, "The mapper function cannot be null");
        if (concurrency <= 0) {
            throw new IllegalArgumentException("Concurrency must be positive, was " + concurrency);
        }
        if (prefetch <= 0) {
            throw new IllegalArgumentException("Prefetch must be positive, was " + prefetch);
        }
        this.concurrency = concurrency;
        this.prefetch = prefetch;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        SpreadProcessor processor = new SpreadProcessor(subscriber);
        subscriber.onSubscribe(processor);
        upstream.subscribe(processor);
    }

    private class SpreadProcessor implements Flow.Subscriber<T>, Flow.Subscription {

        private final Flow.Subscriber<? super R> downstream;
        private volatile Flow.@Nullable Subscription upstreamSubscription;

        private final AtomicBoolean cancelled = new AtomicBoolean();
        private final AtomicLong demand = new AtomicLong();
        private final AtomicInteger wip = new AtomicInteger();
        private final AtomicInteger activeCount = new AtomicInteger();

        private final ConcurrentLinkedQueue<R> queue = new ConcurrentLinkedQueue<>();
        private final ConcurrentLinkedQueue<InnerSubscriber> innerSubscribers = new ConcurrentLinkedQueue<>();

        private final AtomicReference<@Nullable Throwable> error = new AtomicReference<>();
        private volatile boolean upstreamDone;

        SpreadProcessor(Flow.Subscriber<? super R> downstream) {
            this.downstream = downstream;
        }

        // ---- Flow.Subscriber<T> (upstream) ---- //

        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.upstreamSubscription = subscription;
            if (cancelled.get()) {
                subscription.cancel();
                return;
            }
            subscription.request(concurrency);
        }

        @Override
        public void onNext(T item) {
            if (cancelled.get() || error.get() != null) {
                return;
            }
            Flow.Publisher<R> inner;
            try {
                inner = mapper.apply(item);
            } catch (Throwable t) {
                onError(t);
                return;
            }
            if (inner == null) {
                onError(new NullPointerException("The mapper returned a null publisher"));
                return;
            }
            activeCount.incrementAndGet();
            InnerSubscriber innerSubscriber = new InnerSubscriber();
            innerSubscribers.offer(innerSubscriber);
            inner.subscribe(innerSubscriber);
        }

        @Override
        public void onError(Throwable throwable) {
            if (error.compareAndSet(null, throwable)) {
                upstreamDone = true;
                Flow.Subscription us = upstreamSubscription;
                if (us != null) {
                    us.cancel();
                }
                cancelAllInners();
                drain();
            }
        }

        @Override
        public void onComplete() {
            upstreamDone = true;
            drain();
        }

        // ---- Flow.Subscription (downstream) ---- //

        @Override
        public void request(long n) {
            if (cancelled.get()) {
                return;
            }
            if (n <= 0L) {
                onError(Helper.negativeRequest(n));
            } else {
                Helper.add(demand, n);
                drain();
            }
        }

        @Override
        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                Flow.Subscription us = upstreamSubscription;
                if (us != null) {
                    us.cancel();
                }
                cancelAllInners();
                queue.clear();
            }
        }

        // ---- Drain loop ---- //

        private void drain() {
            if (wip.getAndIncrement() != 0) {
                return;
            }
            int missed = 1;
            while (true) {
                if (cancelled.get()) {
                    queue.clear();
                    return;
                }

                Throwable err = error.get();
                if (err != null) {
                    queue.clear();
                    cancelled.set(true);
                    downstream.onError(err);
                    return;
                }

                long emitted = 0L;
                long currentDemand = demand.get();

                while (emitted < currentDemand) {
                    if (cancelled.get()) {
                        queue.clear();
                        return;
                    }
                    err = error.get();
                    if (err != null) {
                        queue.clear();
                        cancelled.set(true);
                        downstream.onError(err);
                        return;
                    }
                    R item = queue.poll();
                    if (item == null) {
                        break;
                    }
                    downstream.onNext(item);
                    emitted++;
                }

                if (emitted > 0L) {
                    if (currentDemand != Long.MAX_VALUE) {
                        demand.addAndGet(-emitted);
                    }
                }

                if (cancelled.get()) {
                    queue.clear();
                    return;
                }

                if (upstreamDone && activeCount.get() == 0 && queue.isEmpty()) {
                    err = error.get();
                    cancelled.set(true);
                    if (err != null) {
                        downstream.onError(err);
                    } else {
                        downstream.onComplete();
                    }
                    return;
                }

                missed = wip.addAndGet(-missed);
                if (missed == 0) {
                    return;
                }
            }
        }

        private void cancelAllInners() {
            InnerSubscriber inner;
            while ((inner = innerSubscribers.poll()) != null) {
                inner.cancel();
            }
        }

        // ---- Inner subscriber ---- //

        private class InnerSubscriber implements Flow.Subscriber<R> {

            private volatile Flow.@Nullable Subscription subscription;
            private int produced;
            private final int limit = prefetch - (prefetch >> 2);

            @Override
            public void onSubscribe(Flow.Subscription s) {
                this.subscription = s;
                if (cancelled.get() || error.get() != null) {
                    s.cancel();
                    return;
                }
                s.request(prefetch);
            }

            @Override
            public void onNext(R item) {
                if (cancelled.get() || error.get() != null) {
                    return;
                }
                queue.offer(item);
                produced++;
                if (produced >= limit) {
                    Flow.Subscription s = subscription;
                    if (s != null) {
                        s.request(produced);
                    }
                    produced = 0;
                }
                drain();
            }

            @Override
            public void onError(Throwable throwable) {
                SpreadProcessor.this.onError(throwable);
            }

            @Override
            public void onComplete() {
                activeCount.decrementAndGet();
                innerSubscribers.remove(this);
                if (!upstreamDone && !cancelled.get()) {
                    Flow.Subscription us = upstreamSubscription;
                    if (us != null) {
                        us.request(1);
                    }
                }
                drain();
            }

            void cancel() {
                Flow.Subscription s = subscription;
                if (s != null) {
                    s.cancel();
                }
            }
        }
    }
}
