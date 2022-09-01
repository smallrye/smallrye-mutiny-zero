package mutiny.zero;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Flow;

/**
 * A pipeline with a source {@link Flow.Publisher} and multiple chained {@link Flow.Processor}.
 * The resulting pipeline is itself a {@link Flow.Publisher} that can be subscribed to.
 *
 * @param <T> the emitted items type
 */
public final class Pipeline<T> implements Flow.Publisher<T> {

    private final Flow.Publisher<?> source;
    private final List<Flow.Processor<?, ?>> processors;

    private Pipeline(Flow.Publisher<T> source) {
        this.source = source;
        processors = List.of();
    }

    private Pipeline(Flow.Publisher<?> source, List<Flow.Processor<?, ?>> processors) {
        this.source = source;
        this.processors = processors;
    }

    /**
     * Create a new pipeline from a source {@link Flow.Publisher}.
     *
     * @param source the source {@link Flow.Publisher}
     * @param <T> the emitted items type
     * @return a new instance
     */
    public static <T> Pipeline<T> create(Flow.Publisher<T> source) {
        return new Pipeline<>(requireNonNull(source, "The source cannot be null"));
    }

    /**
     * Create a new pipeline that extends the current one with a new processor at the subscriber end side.
     *
     * @param processor the new processor
     * @param <R> the processor return type
     * @return a new pipeline
     */
    @SuppressWarnings("unchecked")
    public <R> Pipeline<R> chain(Flow.Processor<T, R> processor) {
        ArrayList<Flow.Processor<?, ?>> extended = new ArrayList<>(processors);
        extended.add(requireNonNull(processor, "The processor cannot be null"));
        return new Pipeline<>(source, extended);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public void subscribe(Flow.Subscriber<? super T> subscriber) {
        // The API provides for a type-safe assembly, so we can afford lifting type checks below
        Flow.Subscriber current = requireNonNull(subscriber, "The subscriber cannot be null");
        for (int i = processors.size() - 1; i >= 0; i--) {
            Flow.Processor processor = processors.get(i);
            processor.subscribe(current);
            current = processor;
        }
        source.subscribe(current);
    }
}
