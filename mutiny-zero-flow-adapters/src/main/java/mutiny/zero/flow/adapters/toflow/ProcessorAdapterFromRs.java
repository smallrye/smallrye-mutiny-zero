package mutiny.zero.flow.adapters.toflow;

import java.util.concurrent.Flow;

import org.reactivestreams.Processor;

import mutiny.zero.flow.adapters.AdaptersToReactiveStreams;
import mutiny.zero.flow.adapters.common.Wrapper;

public class ProcessorAdapterFromRs<T, R> implements Flow.Processor<T, R>, Wrapper<Processor<T, R>> {

    private final Processor<T, R> processor;

    public ProcessorAdapterFromRs(Processor<T, R> processor) {
        this.processor = processor;
    }

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        processor.subscribe(AdaptersToReactiveStreams.subscriber(subscriber));
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        processor.onSubscribe(AdaptersToReactiveStreams.subscription(subscription));
    }

    @Override
    public void onNext(T item) {
        processor.onNext(item);
    }

    @Override
    public void onError(Throwable throwable) {
        processor.onError(throwable);
    }

    @Override
    public void onComplete() {
        processor.onComplete();
    }

    @Override
    public Processor<T, R> unwrap() {
        return processor;
    }
}
