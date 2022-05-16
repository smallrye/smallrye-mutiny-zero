package mutiny.zero.flow.adapters;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Flow;

import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import mutiny.zero.flow.adapters.common.Wrapper;
import mutiny.zero.flow.adapters.tors.ProcessorAdapterFromFlow;
import mutiny.zero.flow.adapters.tors.PublisherAdapterFromFlow;
import mutiny.zero.flow.adapters.tors.SubscriberAdapterFromFlow;
import mutiny.zero.flow.adapters.tors.SubscriptionAdapterFromFlow;

/**
 * Adapters from {@link Flow} types to Reactive Streams types.
 */
@SuppressWarnings("unchecked")
public interface AdaptersToReactiveStreams {

    /**
     * Convert a {@link Flow.Publisher} to a {@link Publisher}.
     *
     * @param publisher the publisher
     * @param <T> the items type
     * @return the wrapped publisher
     */
    static <T> Publisher<T> publisher(Flow.Publisher<T> publisher) {
        requireNonNull(publisher, "The publisher must not be null");
        if (publisher instanceof Wrapper) {
            return (Publisher<T>) ((Wrapper<?>) publisher).unwrap();
        } else {
            return new PublisherAdapterFromFlow<>(publisher);
        }
    }

    /**
     * Convert a {@link Flow.Subscriber} to a {@link Subscriber}.
     *
     * @param subscriber the subscriber
     * @param <T> the items type
     * @return the wrapped subscriber
     */
    static <T> Subscriber<T> subscriber(Flow.Subscriber<T> subscriber) {
        requireNonNull(subscriber, "The subscriber must not be null");
        if (subscriber instanceof Wrapper) {
            return (Subscriber<T>) ((Wrapper<?>) subscriber).unwrap();
        } else {
            return new SubscriberAdapterFromFlow<>(subscriber);
        }
    }

    /**
     * Convert a {@link Flow.Subscription} to a {@link Subscription}.
     *
     * @param subscription the subscription
     * @return the wrapped subscription
     */
    static Subscription subscription(Flow.Subscription subscription) {
        requireNonNull(subscription, "The subscription must not be null");
        if (subscription instanceof Wrapper) {
            return (Subscription) ((Wrapper<?>) subscription).unwrap();
        } else {
            return new SubscriptionAdapterFromFlow(subscription);
        }
    }

    /**
     * Convert a {@link Flow.Processor} to a {@link Processor}.
     *
     * @param processor the processor
     * @param <T> the items type
     * @param <R> the output items type
     * @return the wrapped processor
     */
    static <T, R> Processor<T, R> processor(Flow.Processor<T, R> processor) {
        requireNonNull(processor, "The processor must not be null");
        if (processor instanceof Wrapper) {
            return (Processor<T, R>) ((Wrapper<?>) processor).unwrap();
        } else {
            return new ProcessorAdapterFromFlow<>(processor);
        }
    }
}
