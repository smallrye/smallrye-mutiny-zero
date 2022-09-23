package docsamples;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import mutiny.zero.vertxpublishers.VertxPublisher;

public class UrbanObservatoryHttpClient {

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();
        ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

        RequestOptions opts = new RequestOptions()
                .setSsl(true)
                .setHost("api.usb.urbanobservatory.ac.uk")
                .setPort(443)
                .setMethod(HttpMethod.GET)
                .addHeader("Accept", "application/json")
                .setURI("/api/v2.0a/sensors/entity");

        HttpClient httpClient = vertx.createHttpClient();

        Flow.Publisher<Buffer> publisher = VertxPublisher.fromFuture(() -> httpClient
                .request(opts)
                .compose(HttpClientRequest::send));

        publisher.subscribe(new Flow.Subscriber<>() {

            private Flow.Subscription subscription;

            @Override
            public void onSubscribe(Flow.Subscription s) {
                System.out.println("======================================");
                this.subscription = s;
                s.request(1L);
            }

            @Override
            public void onNext(Buffer buffer) {
                System.out.print(buffer.toString(StandardCharsets.UTF_8));
                executor.schedule(() -> subscription.request(1L), 500, TimeUnit.MILLISECONDS);
            }

            @Override
            public void onError(Throwable t) {
                System.out.println("======================================");
                t.printStackTrace();
            }

            @Override
            public void onComplete() {
                System.out.println("======================================");
            }
        });
    }
}
