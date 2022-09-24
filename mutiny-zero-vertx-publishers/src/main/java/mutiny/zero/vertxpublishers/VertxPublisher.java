package mutiny.zero.vertxpublishers;

import static java.util.Objects.requireNonNull;

import java.util.concurrent.Flow.Publisher;
import java.util.function.Supplier;

import io.vertx.core.Future;
import io.vertx.core.streams.ReadStream;

/**
 * Expose Vert.x streams as Reactive Streams compliant publishers.
 */
public interface VertxPublisher {

    /**
     * Create a publisher from a Vert.x stream supplier.
     * The supplier is called for each new publisher subscription.
     *
     * @param streamSupplier the {@link ReadStream} supplier, cannot be {@code null}, cannot return {@code null}, must not throw
     *        an exception
     * @param <T> the elements type
     * @return the new {@link Publisher}
     */
    static <T> Publisher<T> fromSupplier(Supplier<ReadStream<T>> streamSupplier) {
        requireNonNull(streamSupplier, "The stream supplier cannot be null");
        return new SuppliedStreamPublisher<>(streamSupplier);
    }

    /**
     * Create a publisher from a Vert.x future stream supplier.
     * The supplier is called for each new publisher subscription.
     *
     * @param futureStreamSupplier the {@link Future} {@link ReadStream} supplier, cannot be {@code null}, cannot return
     *        {@code null}, must not throw an exception
     * @param <T> the elements type
     * @return the new {@link Publisher}
     */
    static <T> Publisher<T> fromFuture(Supplier<Future<? extends ReadStream<T>>> futureStreamSupplier) {
        requireNonNull(futureStreamSupplier, "The future supplier cannot be null");
        return new SuppliedFutureStreamPublisher<>(futureStreamSupplier);
    }
}
