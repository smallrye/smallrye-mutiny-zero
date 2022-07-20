package mutiny.zero.internal;

import java.util.concurrent.Flow;

public class AlreadyCompletedSubscription implements Flow.Subscription {

    @Override
    public void request(long n) {
        // Do nothing
    }

    @Override
    public void cancel() {
        // Do nothing
    }
}
