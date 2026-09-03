package mutiny.zero.internal;

import java.util.Queue;
import java.util.concurrent.Flow;

public abstract class BufferingTubeBase<T> extends TubeBase<T> {

    protected volatile boolean delayedComplete = false;

    public BufferingTubeBase(Flow.Subscriber<? super T> subscriber) {
        super(subscriber);
    }

    abstract Queue<T> overflowQueue();

    @Override
    public void request(long n) {
        if (cancelled) {
            return;
        }
        if (n <= 0L) {
            fail(Helper.negativeRequest(n));
        } else {
            Helper.add(requested, n);
            requestConsumer.accept(n);
        }
        drainLoop();
    }

    @Override
    protected void drainOverflow() {
        long remaining = outstandingRequests();
        if (remaining <= 0L) {
            return;
        }
        T bufferedItem;
        do {
            bufferedItem = overflowQueue().poll();
            if (bufferedItem != null) {
                dispatchQueue.offer(bufferedItem);
                remaining--;
            }
        } while (bufferedItem != null && remaining > 0L);

        if (!completed && delayedComplete && overflowQueue().isEmpty()) {
            completed = true;
        }
    }

    @Override
    public void complete() {
        if (overflowQueue().isEmpty()) {
            super.complete();
        } else {
            delayedComplete = true;
        }
    }
}
