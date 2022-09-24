# Vert.x-based Reactive Streams Publishers

The `mutiny-zero-vertx-publishers` library (Maven coordinates `io.smallrye.reactive:mutiny-zero-vertx-publishers`) allows creating _Reactive Streams_ publishers from [Vert.x](https://vertx.io/) streams.

This library acts as a thin adapter between Vert.x `ReadStream` and `java.util.concurrent.Flow.Publisher` and uses _Mutiny Zero_ to expose _Reactive Streams_ compliant publishers.

## API overview

The entry point is the `mutiny.zero.vertxpublishers.VertxPublisher` interface that exposes 2 static factory methods.

- `Publisher<T> fromSupplier(Supplier<ReadStream<T>> streamSupplier)` is to be used when some Vert.x API returns a `ReadStream<T>`.
- `Publisher<T> fromFuture(Supplier<Future<? extends ReadStream<T>>> futureStreamSupplier)` is to be used when some Vert.x API asynchronously returns a `ReadStream<T>` through a `Future`.

The factory methods use suppliers so that the `ReadStream` instances to be adapted are on a per-subscriber basis.

## Sample usage

The following example makes HTTP requests to the [Newcastle University Urban Observatory API](https://api.usb.urbanobservatory.ac.uk/) using the Vert.x HTTP client:

```java linenums="1"
--8<-- "mutiny-zero-vertx-publishers/src/test/java/docsamples/UrbanObservatoryHttpClient.java"
```

A new HTTP connection is issued everytime the publisher is being subscribed.
In this example the subscriber controls demand by requesting a new Vert.x `Buffer` every 500ms.

If you run this program then you will see the JSON response being progressively printed to the standard console in chunks, every 500 milliseconds.  
