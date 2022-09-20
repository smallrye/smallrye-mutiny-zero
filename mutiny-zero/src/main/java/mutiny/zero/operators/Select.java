package mutiny.zero.operators;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Flow;
import java.util.function.Predicate;

/**
 * A {@link java.util.concurrent.Flow.Publisher} that selects elements matching a {@link Predicate}.
 *
 * @param <T> the elements type
 */
public class Select<T> implements Flow.Publisher<T> {

    private final Flow.Publisher<T> upstream;
    private final Predicate<T> predicate;

    /**
     * Build a new selection publisher.
     *
     * @param upstream the upstream publisher
     * @param predicate the predicate to select the elements forwarded to subscribers, must not throw exceptions
     */
    public Select(Flow.Publisher<T> upstream, Predicate<T> predicate) {
        this.upstream = requireNonNull(upstream, "The upstream cannot be null");
        this.predicate = requireNonNull(predicate, "The predicate cannot be null");
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        Processor processor = new Processor();
        processor.subscribe(subscriber);
        upstream.subscribe(processor);
    }

    private class Processor extends ProcessorBase<T, T> {

        @Override
        public void onNext(T item) {
            if (!cancelled()) {
                try {
                    if (predicate.test(item)) {
                        downstream().onNext(item);
                    }
                } catch (Throwable failure) {
                    cancel();
                    downstream().onError(failure);
                }
            }
        }
    }
}
