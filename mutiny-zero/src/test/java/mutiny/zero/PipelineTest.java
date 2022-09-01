package mutiny.zero;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.Flow;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.helpers.test.AssertSubscriber;

class PipelineTest {

    @Test
    @DisplayName("Reject a null source")
    void rejectNullSource() {
        assertThatThrownBy(() -> Pipeline.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Reject a null processor")
    void rejectNullProcessor() {
        assertThatThrownBy(() -> Pipeline.create(Multi.createFrom().item(1)).chain(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("cannot be null");
    }

    @Test
    @DisplayName("Test a pipeline assembly with no processor")
    void assemblyNoProcessor() {
        Pipeline<Integer> pipeline = Pipeline.create(Multi.createFrom().items(1, 2, 3));
        AssertSubscriber<Integer> sub = new AssertSubscriber<>(Long.MAX_VALUE);
        pipeline.subscribe(sub);
        sub.assertCompleted().assertItems(1, 2, 3);
    }

    @Test
    @DisplayName("Test a pipeline assembly with multiple processors")
    void assemblyWithProcessors() {
        Pipeline<String> pipeline = Pipeline.create(Multi.createFrom().items(1, 2, 3))
                .chain(new TenTimes())
                .chain(new IntegerToString())
                .chain(new DecorateString());

        AssertSubscriber<String> sub = new AssertSubscriber<>(Long.MAX_VALUE);
        pipeline.subscribe(sub);
        sub.assertCompleted().assertItems("[10]", "[20]", "[30]");
    }

    @Test
    @DisplayName("Test a pipeline assembly immutability ")
    void immutableAssemblies() {
        Pipeline<Integer> first = Pipeline.create(Multi.createFrom().items(1, 2, 3))
                .chain(new TenTimes());

        Pipeline<String> second = first
                .chain(new IntegerToString())
                .chain(new DecorateString());

        AssertSubscriber<Integer> sub1 = new AssertSubscriber<>(Long.MAX_VALUE);
        first.subscribe(sub1);
        sub1.assertCompleted().assertItems(10, 20, 30);

        AssertSubscriber<String> sub2 = new AssertSubscriber<>(Long.MAX_VALUE);
        second.subscribe(sub2);
        sub2.assertCompleted().assertItems("[10]", "[20]", "[30]");
    }
}

// Note: the code below is not meant to be RS-compliant

abstract class ForwardingProcessorBase<T, R> implements Flow.Processor<T, R> {

    Flow.Subscription upstream;
    Flow.Subscriber<? super R> downstream;

    @Override
    public void subscribe(Flow.Subscriber<? super R> subscriber) {
        downstream = subscriber;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        upstream = subscription;
        downstream.onSubscribe(subscription);
    }

    @Override
    public void onError(Throwable throwable) {
        downstream.onError(throwable);
    }

    @Override
    public void onComplete() {
        downstream.onComplete();
    }
}

class IntegerToString extends ForwardingProcessorBase<Integer, String> {

    @Override
    public void onNext(Integer item) {
        downstream.onNext(item.toString());
    }
}

class TenTimes extends ForwardingProcessorBase<Integer, Integer> {

    @Override
    public void onNext(Integer item) {
        downstream.onNext(item * 10);
    }
}

class DecorateString extends ForwardingProcessorBase<String, String> {

    @Override
    public void onNext(String item) {
        downstream.onNext("[" + item + "]");
    }
}
