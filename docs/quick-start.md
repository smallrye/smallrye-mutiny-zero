# Quick start

## Getting Mutiny Zero

You can get Mutiny Zero through the following Maven coordinates:

* `groupId`: `io.smallrye.reactive`
* `artifactId`: `mutiny-zero`
  
Mutiny Zero exposes publishers (see [`java.util.concurrent.Flow`](https://docs.oracle.com/en/java/javase/11/docs/api/java.base/java/util/concurrent/Flow.html)).

## Creating publishers

Your main entry point in the Mutiny Zero API is the `mutiny.zero.ZeroPublisher` interface that exposes factory methods.

### Creating from known values

If you already know the values to be emitted (or a failure), then you can use following factory methods:

```java linenums="1"
--8<-- "mutiny-zero/src/test/java/docsamples/FromKnownValues.java"
```

### Creating from `CompletionStage`

`CompletionStage` is the Java SDK API for asynchronous operations that emit either a result or a failure.

Mutiny Zero can create a `Publisher` from a `CompletionStage` that emits exactly 1 item or a failure, then a completion signal:

```java linenums="1"
--8<-- "mutiny-zero/src/test/java/docsamples/FromCompletionStage.java"
```

### Creating using the general-purpose `Tube` API

For all other cases you should use the `mutiny.zero.Tube` API.

A `Tube` represents an object you can interact with to emit values, error and completion.
It is aware of cancellation and items requests made by the subscriber.

A `Tube` is a good abstraction if you want to pass events from an existing asynchronous I/O source as they arrive.
Here is a not so fictional example where `SampleAsyncSource` (an asynchronous I/O API) has to be adapted to a `Publisher`:

```java linenums="1"
--8<-- "mutiny-zero/src/test/java/docsamples/FromTube.java"
```

Since `SampleAsyncSource` does not support reactive streams but can be paused and resumed, the `Tube` API is used not just to send items but also to control `SampleAsyncSource`.
The `Tube` also has a buffer of 256 items in case of overflow to cope with the fact that pausing `SampleAsyncSource` may not be immediate.

Several back-pressure strategies are offered by `Tube`:

* buffer items (with or without a bound),
* drop items,
* signal an error,
* ignore back-pressure and still send items,
* keep only the last values in a fixed-size buffer.

`Tube` enforces the reactive streams protocol.
For instance if you have several threads competing to send items, then items will still be emitted _serially_ by one of the threads rather than concurrently.

## Helpers

Mutiny Zero offers a few helpers for commonly-needed tasks, but without the intention of becoming a full-fledge reactive programming API.

Introducing new helpers will always be done carefully by observing real world implementation patterns.

### `CompletionStage` helpers

The `AsyncHelpers` class offers a few helpers to simplify developing against the `CompletionStage`, especially before Java 11.

* `failedFuture` creates a `CompletionStage` that has already failed.
* `applyExceptionally` applies a function to map a failure `Throwable` to another `Throwable`.
* `composeExceptionally` applies a function to compose a failure `Throwable` to another `CompletionStage`.

### `Publisher` helpers

More often than not we need a little help when dealing with a `Publisher`.

* `collectToList` assembles all items from a `Publisher` to a `CompletionStage<List>`.
* `map` returns a `Publisher` that applies a function to all items from the original `Publisher`. 
