package mutiny.zero.internal;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class CompletionStagePublisher<T> implements Publisher<T> {

    private final Supplier<CompletionStage<T>> completionStageSupplier;

    public CompletionStagePublisher(Supplier<CompletionStage<T>> completionStageSupplier) {
        this.completionStageSupplier = completionStageSupplier;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        subscriber.onSubscribe(new CompletionStageSubscription<>(completionStageSupplier, subscriber));
    }

    private static class CompletionStageSubscription<T> implements Flow.Subscription {

        private final Supplier<CompletionStage<T>> completionStageSupplier;
        private final Subscriber<? super T> subscriber;
        private final AtomicReference<State> state = new AtomicReference<>(State.INIT);
        private CompletableFuture<T> completableFuture;

        enum State {
            INIT,
            ACTIVE,
            DONE
        }

        public CompletionStageSubscription(Supplier<CompletionStage<T>> completionStageSupplier,
                Subscriber<? super T> subscriber) {
            this.completionStageSupplier = completionStageSupplier;
            this.subscriber = subscriber;
        }

        @Override
        public void request(long n) {
            if (n <= 0L) {
                cancel();
                subscriber.onError(Helper.negativeRequest(n));
                return;
            }
            if (state.compareAndSet(State.INIT, State.ACTIVE)) {
                CompletionStage<T> cs = completionStageSupplier.get();
                if (cs == null) {
                    state.set(State.DONE);
                    subscriber.onError(new NullPointerException("The completion stage is null"));
                    return;
                }
                completableFuture = cs.toCompletableFuture();
                completableFuture.whenComplete((value, err) -> {
                    if (state.getAndSet(State.DONE) == State.ACTIVE) {
                        if (err != null) {
                            subscriber.onError(err);
                        } else if (value == null) {
                            subscriber.onError(new NullPointerException("The CompletionStage produced a null value"));
                        } else {
                            subscriber.onNext(value);
                            subscriber.onComplete();
                        }
                    }
                });
            }
        }

        @Override
        public void cancel() {
            if (state.getAndSet(State.DONE) != State.DONE) {
                if (completableFuture != null) {
                    completableFuture.cancel(false);
                }
            }
        }
    }
}
