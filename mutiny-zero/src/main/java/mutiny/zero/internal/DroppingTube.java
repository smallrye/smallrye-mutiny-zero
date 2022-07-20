package mutiny.zero.internal;

import java.util.concurrent.Flow;

public class DroppingTube<T> extends TubeBase<T> {

    protected DroppingTube(Flow.Subscriber<? super T> subscriber) {
        super(subscriber);
    }

    @Override
    protected void handleItem(T item) {
        if (outstandingRequests() > 0L) {
            dispatchQueue.offer(item);
            drainLoop();
        }
    }
}
