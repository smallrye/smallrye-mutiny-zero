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

@SuppressWarnings("unchecked")
public interface AdaptersToReactiveStreams {

    static <T> Publisher<T> publisher(Flow.Publisher<T> publisher) {
        requireNonNull(publisher, "The publisher must not be null");
        if (publisher instanceof Wrapper) {
            return (Publisher<T>) ((Wrapper<?>) publisher).unwrap();
        } else {
            return new PublisherAdapterFromFlow<>(publisher);
        }
    }

    static <T> Subscriber<T> subscriber(Flow.Subscriber<T> subscriber) {
        requireNonNull(subscriber, "The subscriber must not be null");
        if (subscriber instanceof Wrapper) {
            return (Subscriber<T>) ((Wrapper<?>) subscriber).unwrap();
        } else {
            return new SubscriberAdapterFromFlow<>(subscriber);
        }
    }

    static Subscription subscription(Flow.Subscription subscription) {
        requireNonNull(subscription, "The subscription must not be null");
        if (subscription instanceof Wrapper) {
            return (Subscription) ((Wrapper<?>) subscription).unwrap();
        } else {
            return new SubscriptionAdapterFromFlow(subscription);
        }
    }

    static <T, R> Processor<T, R> processor(Flow.Processor<T, R> processor) {
        requireNonNull(processor, "The processor must not be null");
        if (processor instanceof Wrapper) {
            return (Processor<T, R>) ((Wrapper<?>) processor).unwrap();
        } else {
            return new ProcessorAdapterFromFlow<>(processor);
        }
    }
}
