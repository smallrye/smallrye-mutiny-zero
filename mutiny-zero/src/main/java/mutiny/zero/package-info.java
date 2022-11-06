/**
 * Mutiny Zero is minimal API for creating reactive streams compliant {@link java.util.concurrent.Flow.Publisher} objects.
 * <p>
 * {@link mutiny.zero.ZeroPublisher} offers factory methods for creating {@link java.util.concurrent.Flow.Publisher},
 * with the {@link mutiny.zero.Tube} API and the
 * {@link mutiny.zero.ZeroPublisher#create(mutiny.zero.TubeConfiguration, java.util.function.Consumer)}
 * factory method being the safe, general-purpose choice.
 * <p>
 * Other factory methods provide simple abstractions over in-memory data structures and special cases.
 * There is also a bridge with the {@link java.util.concurrent.CompletionStage} APIs.
 */
package mutiny.zero;