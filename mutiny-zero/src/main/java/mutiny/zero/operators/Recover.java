package mutiny.zero.operators;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Flow;
import java.util.function.Function;

/**
 * A {@link java.util.concurrent.Flow.Publisher} that recovers from failure using a {@link Function}.
 * <p>
 * The provided function accepts an error that would normally trigger an
 * {@link java.util.concurrent.Flow.Subscriber#onError(Throwable)} signal.
 * <p>
 * The function returns a recovery value of type {@code T}, then the stream terminates with an
 * {@link java.util.concurrent.Flow.Subscriber#onComplete()} signal.
 * If the function returns {@code null} then the stream terminates directly with a completion event.
 * <p>
 * The stream ends with an error if the function throws an exception.
 *
 * @param <T> the elements type
 */
public class Recover<T> implements Flow.Publisher<T> {

    private final Flow.Publisher<T> upstream;
    private final Function<Throwable, T> function;

    /**
     * Build a new recovery publisher.
     *
     * @param upstream the upstream publisher
     * @param function the recovery function, must not return {@code null} values
     */
    public Recover(Flow.Publisher<T> upstream, Function<Throwable, T> function) {
        this.upstream = requireNonNull(upstream, "The upstream cannot be null");
        ;
        this.function = requireNonNull(function, "The function cannot be null");
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
                downstream().onNext(item);
            }
        }

        @Override
        public void onError(Throwable err) {
            if (!cancelled()) {
                cancel();
                Flow.Subscriber<? super T> downstream = downstream();
                try {
                    T finalItem = function.apply(err);
                    if (finalItem == null) {
                        downstream.onComplete();
                    } else {
                        downstream.onNext(finalItem);
                        downstream.onComplete();
                    }
                } catch (Throwable functionErr) {
                    downstream.onError(functionErr);
                }
            }
        }
    }
}
