/*
 * Copyright (c) Octavia Togami <https://octyl.net>
 * Copyright (c) contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.octyl.rss.dokusha.util;

import java.time.Duration;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MoreFlux {
    public static <T, E> Function<Flux<T>, Flux<E>> instanceOf(Class<E> clazz) {
        return f -> f.handle((t, sink) -> {
            if (clazz.isInstance(t)) {
                sink.next(clazz.cast(t));
            }
        });
    }

    public static <T> Function<Flux<T>, Flux<T>> throttle(Duration minDuration) {
        return f -> f.zipWith(Flux.interval(Duration.ZERO, minDuration).onBackpressureDrop(), (v, __) -> v);
    }

    private MoreFlux() {
    }
}
