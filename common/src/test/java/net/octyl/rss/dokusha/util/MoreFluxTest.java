package net.octyl.rss.dokusha.util;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

public class MoreFluxTest {
    @Test
    void instanceOf() {
        StepVerifier.withVirtualTime(
            () -> Flux.just("a", 1, 1.0f).transform(MoreFlux.instanceOf(String.class))
        )
            .expectSubscription()
            .expectNext("a")
            .verifyComplete();
        StepVerifier.withVirtualTime(
            () -> Flux.just("a", 1, 1.0f).transform(MoreFlux.instanceOf(Integer.class))
        )
            .expectSubscription()
            .expectNext(1)
            .verifyComplete();
        StepVerifier.withVirtualTime(
            () -> Flux.just("a", 1, 1.0f).transform(MoreFlux.instanceOf(Object.class))
        )
            .expectSubscription()
            .expectNext("a")
            .expectNext(1)
            .expectNext(1.0f)
            .verifyComplete();
    }

    @Test
    void throttle() {
        StepVerifier.withVirtualTime(
            () -> Flux.just("alpha", "beta", "gamma").transform(MoreFlux.throttle(Duration.ofSeconds(1)))
        )
            .expectSubscription()
            .expectNext("alpha")
            .expectNoEvent(Duration.ofSeconds(1))
            .expectNext("beta")
            .expectNoEvent(Duration.ofSeconds(1))
            .expectNext("gamma")
            .verifyComplete();
    }
}
