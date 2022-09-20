package mutiny.zero;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Function;

import mutiny.zero.operators.Transform;

public interface PublisherHelpers {

    /**
     * Collect all items as a list.
     *
     * @param publisher the publisher
     * @param <T> the emitted type
     * @return the future accumulating the items into a list.
     */
    static <T> CompletionStage<List<T>> collectToList(Publisher<T> publisher) {
        List<T> list = new ArrayList<>();
        CompletableFuture<List<T>> future = new CompletableFuture<>();
        publisher.subscribe(new Subscriber<T>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(T t) {
                list.add(t);
            }

            @Override
            public void onError(Throwable throwable) {
                future.completeExceptionally(throwable);
            }

            @Override
            public void onComplete() {
                future.complete(list);
            }
        });
        return future;
    }

    /**
     * Simple map implementation.
     *
     * @param source the upstream
     * @param mapper the mapper
     * @param <I> the input type
     * @param <O> the output type
     * @return the mapped publisher.
     * @deprecated Use {@link Transform} instead
     */
    @Deprecated(forRemoval = true, since = "0.5.0")
    static <I, O> Publisher<O> map(Publisher<I> source, Function<I, O> mapper) {
        requireNonNull(source, "The source cannot be null");
        requireNonNull(mapper, "The mapper cannot be null");
        return new Transform<>(source, mapper);
    }

}
