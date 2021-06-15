package docsamples;

import org.reactivestreams.Publisher;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static mutiny.zero.ZeroPublisher.fromCompletionStage;

public class FromCompletionStage {

    public static void main(String[] args) {

        Publisher<Long> pun = fromCompletionStage(() -> supplyAsync(() -> 58L));
    }
}
