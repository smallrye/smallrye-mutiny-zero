package mutiny.zero.internal;

import java.util.Queue;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.LinkedBlockingDeque;

public class BufferingTube<T> extends BufferingTubeBase<T> {

    private final LinkedBlockingDeque<T> overflowQueue;

    public BufferingTube(Subscriber<? super T> subscriber, int bufferSize) {
        super(subscriber);
        overflowQueue = new LinkedBlockingDeque<>(bufferSize);
    }

    @Override
    Queue<T> overflowQueue() {
        return overflowQueue;
    }

    @Override
    protected void handleItem(T item) {
        if (outstandingRequests() > 0L) {
            dispatchQueue.offer(item);
            drainLoop();
        } else if (!overflowQueue.offer(item)) {
            fail(new IllegalStateException(
                    "The following item cannot be propagated because there is no demand and the overflow buffer is full: "
                            + item));
        }
    }
}
