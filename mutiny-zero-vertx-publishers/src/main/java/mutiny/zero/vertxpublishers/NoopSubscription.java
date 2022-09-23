package mutiny.zero.vertxpublishers;

import java.util.concurrent.Flow;

class NoopSubscription implements Flow.Subscription {
    @Override
    public void request(long n) {
        // Nothing
    }

    @Override
    public void cancel() {
        // Nothing
    }
}
