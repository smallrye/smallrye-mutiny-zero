package mutiny.zero.flow.adapters;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Flow;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import mutiny.zero.flow.adapters.common.Wrapper;
import mutiny.zero.flow.adapters.toflow.ProcessorAdapterFromRs;
import mutiny.zero.flow.adapters.toflow.PublisherAdapterFromRs;
import mutiny.zero.flow.adapters.toflow.SubscriberAdapterFromRs;
import mutiny.zero.flow.adapters.toflow.SubscriptionAdapterFromRs;

/**
 * Adapters from Reactive Streams types to {@link Flow} types.
 */
@SuppressWarnings("unchecked")
public interface AdaptersToFlow {

    /**
     * Convert a {@link Publisher} to a {@link Flow.Publisher}.
     *
     * @param publisher the publisher
     * @param <T> the items type
     * @return the wrapped publisher
     */
    static <T> Flow.Publisher<T> publisher(Publisher<T> publisher) {
        requireNonNull(publisher, "The publisher must not be null");
        if (publisher instanceof Wrapper) {
            return (Flow.Publisher<T>) ((Wrapper<?>) publisher).unwrap();
        } else {
            return new PublisherAdapterFromRs<>(publisher);
        }
    }

    /**
     * Convert a {@link Subscriber} to a {@link Flow.Subscriber}.
     *
     * @param subscriber the subscriber
     * @param <T> the items type
     * @return the wrapped subscriber
     */
    static <T> Flow.Subscriber<T> subscriber(Subscriber<T> subscriber) {
        requireNonNull(subscriber, "The subscriber must not be null");
        if (subscriber instanceof Wrapper) {
            return (Flow.Subscriber<T>) ((Wrapper<?>) subscriber).unwrap();
        } else {
            return new SubscriberAdapterFromRs<>(subscriber);
        }
    }

    /**
     * Convert a {@link Subscription} to a {@link Flow.Subscription}.
     *
     * @param subscription the subscription
     * @return the wrapped subscription
     */
    static Flow.Subscription subscription(Subscription subscription) {
        requireNonNull(subscription, "The subscription must not be null");
        if (subscription instanceof Wrapper) {
            return (Flow.Subscription) ((Wrapper<?>) subscription).unwrap();
        } else {
            return new SubscriptionAdapterFromRs(subscription);
        }
    }

    /**
     * Convert a {@link Processor} to a {@link Flow.Processor}.
     *
     * @param processor the processor
     * @param <T> the items type
     * @param <R> the output items type
     * @return the wrapped processor
     */
    static <T, R> Flow.Processor<T, R> processor(Processor<T, R> processor) {
        requireNonNull(processor, "The processor must not be null");
        if (processor instanceof Wrapper) {
            return (Flow.Processor<T, R>) ((Wrapper<?>) processor).unwrap();
        }
        return new ProcessorAdapterFromRs<>(processor);
    }
}
