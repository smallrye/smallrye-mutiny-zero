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

@SuppressWarnings("unchecked")
public interface AdaptersToFlow {

    static <T> Flow.Publisher<T> publisher(Publisher<T> publisher) {
        requireNonNull(publisher, "The publisher must not be null");
        if (publisher instanceof Wrapper) {
            return (Flow.Publisher<T>) ((Wrapper<?>) publisher).unwrap();
        } else {
            return new PublisherAdapterFromRs<>(publisher);
        }
    }

    static <T> Flow.Subscriber<T> subscriber(Subscriber<T> subscriber) {
        requireNonNull(subscriber, "The subscriber must not be null");
        if (subscriber instanceof Wrapper) {
            return (Flow.Subscriber<T>) ((Wrapper<?>) subscriber).unwrap();
        } else {
            return new SubscriberAdapterFromRs<>(subscriber);
        }
    }

    static Flow.Subscription subscription(Subscription subscription) {
        requireNonNull(subscription, "The subscription must not be null");
        if (subscription instanceof Wrapper) {
            return (Flow.Subscription) ((Wrapper<?>) subscription).unwrap();
        } else {
            return new SubscriptionAdapterFromRs(subscription);
        }
    }

    static <T, R> Flow.Processor<T, R> processor(Processor<T, R> processor) {
        requireNonNull(processor, "The processor must not be null");
        if (processor instanceof Wrapper) {
            return (Flow.Processor<T, R>) ((Wrapper<?>) processor).unwrap();
        }
        return new ProcessorAdapterFromRs<>(processor);
    }
}
