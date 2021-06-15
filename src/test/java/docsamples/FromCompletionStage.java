package docsamples;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static mutiny.zero.ZeroPublisher.fromCompletionStage;

import org.reactivestreams.Publisher;

public class FromCompletionStage {

    public static void main(String[] args) {

        Publisher<Long> pun = fromCompletionStage(() -> supplyAsync(() -> 58L));
    }
}
