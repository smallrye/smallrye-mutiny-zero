/**
 * A set of adapters from Reactive Streams to/from {@link java.util.concurrent.Flow}.
 * <p>
 * The methods found in {@link mutiny.zero.flow.adapters.AdaptersToFlow} and
 * {@link mutiny.zero.flow.adapters.AdaptersToReactiveStreams}
 * avoid excessive wrapping when possible.
 * For instance wrapping a flow publisher {@code p1} to a reactive streams publisher {@code p2} and back to a flow
 * publisher {@code p3 } yields the original flow publisher where {@code p1 == p3}.
 */
package mutiny.zero.flow.adapters;