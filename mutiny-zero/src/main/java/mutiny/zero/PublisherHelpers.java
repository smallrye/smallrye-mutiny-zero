package mutiny.zero;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;

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
        publisher.subscribe(new Subscriber<>() {
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

}
