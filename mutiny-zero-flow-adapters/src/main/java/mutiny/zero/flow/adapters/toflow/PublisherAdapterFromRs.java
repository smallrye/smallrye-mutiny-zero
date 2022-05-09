package mutiny.zero.flow.adapters.toflow;

import java.util.concurrent.Flow;

import org.reactivestreams.Publisher;

import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;
import mutiny.zero.flow.adapters.common.Wrapper;

public class PublisherAdapterFromRs<T> implements Flow.Publisher<T>, Wrapper<Publisher<T>> {

    private final Publisher<T> publisher;

    public PublisherAdapterFromRs(Publisher<T> publisher) {
        this.publisher = publisher;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        publisher.subscribe(AdaptersToReactiveStreams.subscriber(subscriber));
    }

    @Override
    public Publisher<T> unwrap() {
        return publisher;
    }
}
