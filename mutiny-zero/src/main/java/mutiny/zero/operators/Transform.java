package mutiny.zero.operators;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Flow;
import java.util.function.Function;

/**
 * A {@link java.util.concurrent.Flow.Publisher} that transforms elements using a {@link Function}.
 *
 * @param <I> the input elements type
 * @param <O> the output elements type
 */
public class Transform<I, O> implements Flow.Publisher<O> {

    private final Flow.Publisher<I> upstream;
    private final Function<I, O> function;

    /**
     * Build a new transformation publisher.
     *
     * @param upstream the upstream publisher
     * @param function the transformation function, must not throw exceptions, must not return {@code null} values
     */
    public Transform(Flow.Publisher<I> upstream, Function<I, O> function) {
        this.upstream = requireNonNull(upstream, "The upstream cannot be null");
        ;
        this.function = requireNonNull(function, "The function cannot be null");
    }

    @Override
    public void subscribe(Flow.Subscriber<? super O> subscriber) {
        requireNonNull(subscriber, "The subscriber cannot be null");
        Processor processor = new Processor();
        processor.subscribe(subscriber);
        upstream.subscribe(processor);
    }

    private class Processor extends ProcessorBase<I, O> {

        @Override
        public void onNext(I item) {
            if (!cancelled()) {
                try {
                    O result = function.apply(item);
                    if (result == null) {
                        throw new NullPointerException("The function produced a null result for item " + item);
                    }
                    downstream().onNext(result);
                } catch (Throwable failure) {
                    cancel();
                    downstream().onError(failure);
                }
            }
        }
    }
}
