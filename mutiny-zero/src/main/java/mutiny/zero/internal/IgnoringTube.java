package mutiny.zero.internal;

import java.util.concurrent.Flow;

public class IgnoringTube<T> extends TubeBase<T> {

    public IgnoringTube(Flow.Subscriber<? super T> subscriber) {
        super(subscriber);
    }

    @Override
    protected void handleItem(T item) {
        dispatchQueue.offer(item);
        drainLoop();
    }
}
