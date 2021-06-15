package docsamples;

import org.reactivestreams.Publisher;

import java.io.IOException;
import java.util.Arrays;

import static mutiny.zero.ZeroPublisher.*;

public class FromKnownValues {

    public static void main(String[] args) {

        // From values
        Publisher<Integer> pub1 = fromItems(1, 2, 3);

        // From an iterable
        Publisher<Integer> pub2 = fromIterable(Arrays.asList(1, 2, 3));

        // From a failure
        Publisher<?> pub3 = fromFailure(new IOException("Broken pipe"));
    }
}
