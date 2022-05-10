# JDK Flow from/to Reactive Streams Adapters

The `rs-flow-adapters` library (Maven coordinates `io.smallrye.reactive:rs-flow-adapters`) provides a *clean room* implementation of adapters from and to the [JDK Flow interfaces](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Flow.html) that match those from Reactive Streams.

## Why another adapter library?

The implementation of our adapters is similar in spirit to [those from the Reactive Streams library](https://github.com/reactive-streams/reactive-streams-jvm/tree/master/api/src/main/java9/org/reactivestreams), but they differ by:

- failing early rather than passing `null` through in some cases,
- shipping under a [proper open source license](https://www.apache.org/licenses/LICENSE-2.0) while the Reactive Streams library hasn't made any progress towards publishing a new release, see [#536](https://github.com/reactive-streams/reactive-streams-jvm/issues/536) and [#530](https://github.com/reactive-streams/reactive-streams-jvm/issues/530) 
- having correct JPMS (Java modules) descriptors for those who might need modules rather than the classpath.

## How to use it?

The public API exposes 2 types:

- `AdaptersToFlow` to convert Reactive Streams types to `Flow` types, and
- `AdaptersToReactiveStreams` to convert `Flow` types to Reactive Streams types.

Each type offers factory methods to convert from one type to the other.
For instance here's how you can convert from a Reactive Streams `Publisher` to a `Flow.Publisher`:

```java
Publisher<String> rsPublisher = connect("foo"); // ... where 'connect' returns a Publisher<String>

Flow.Publisher<String> flowPublisher = AdaptersToFlow.publisher(rsPublisher);
```
