package mutiny.zero;

import java.util.function.Consumer;

/**
 * Configuration object for creating {@link Tube} through {@link ZeroPublisher#create(TubeConfiguration, Consumer)}.
 */
public final class TubeConfiguration {

    private BackpressureStrategy backpressureStrategy = BackpressureStrategy.DROP;
    private int bufferSize = -1;

    /**
     * Specify the back-pressure strategy, cannot be {@code null}.
     * The default is {@link BackpressureStrategy#DROP}.
     *
     * @param strategy the back-pressure strategy
     * @return this configuration
     */
    public TubeConfiguration withBackpressureStrategy(BackpressureStrategy strategy) {
        backpressureStrategy = strategy;
        return this;
    }

    /**
     * Specify the buffer size must be strictly positive when {@code backpressureStrategy} is one of
     * {@link BackpressureStrategy#BUFFER} and {@link BackpressureStrategy#LATEST}.
     * The default is {@code -1}.
     *
     * @param bufferSize the buffer size
     * @return this configuration
     */
    public TubeConfiguration withBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
        return this;
    }

    /**
     * Get the back-pressure strategy.
     *
     * @return the back-pressure strategy
     */
    public BackpressureStrategy getBackpressureStrategy() {
        return backpressureStrategy;
    }

    /**
     * Get the buffer size.
     *
     * @return the buffer size
     */
    public int getBufferSize() {
        return bufferSize;
    }
}
