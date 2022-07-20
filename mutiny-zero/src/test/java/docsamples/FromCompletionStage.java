package docsamples;

import static java.util.concurrent.CompletableFuture.supplyAsync;
import static mutiny.zero.ZeroPublisher.fromCompletionStage;

import java.util.concurrent.Flow;

public class FromCompletionStage {

    public static void main(String[] args) {

        Flow.Publisher<Long> pun = fromCompletionStage(() -> supplyAsync(() -> 58L));
    }
}
