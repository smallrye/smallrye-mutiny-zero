package mutiny.zero.flow.adapters.tors;

import java.util.concurrent.Flow;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;

import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.common.Wrapper;

public class PublisherAdapterFromFlow<T> implements Publisher<T>, Wrapper<Flow.Publisher<T>> {

    private final Flow.Publisher<T> publisher;

    public PublisherAdapterFromFlow(Flow.Publisher<T> publisher) {
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        publisher.subscribe(AdaptersToFlow.subscriber(subscriber));
    }

    @Override
    public Flow.Publisher<T> unwrap() {
        return publisher;
    }
}
