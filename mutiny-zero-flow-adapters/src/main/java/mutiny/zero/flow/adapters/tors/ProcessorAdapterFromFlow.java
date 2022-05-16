package mutiny.zero.flow.adapters.tors;

import java.util.concurrent.Flow;

import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import mutiny.zero.flow.adapters.AdaptersToFlow;
import mutiny.zero.flow.adapters.common.Wrapper;

public class ProcessorAdapterFromFlow<T, R> implements Processor<T, R>, Wrapper<Flow.Processor<T, R>> {

    private final Flow.Processor<T, R> processor;

    public ProcessorAdapterFromFlow(Flow.Processor<T, R> processor) {
        this.processor = processor;
    }

    @Override
    public void subscribe(Subscriber<? super R> subscriber) {
        processor.subscribe(AdaptersToFlow.subscriber(subscriber));
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        processor.onSubscribe(AdaptersToFlow.subscription(subscription));
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
    public Flow.Processor<T, R> unwrap() {
        return processor;
    }
}
