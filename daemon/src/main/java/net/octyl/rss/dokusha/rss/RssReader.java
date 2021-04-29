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

package net.octyl.rss.dokusha.rss;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import net.octyl.rss.dokusha.config.RssSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class RssReader {
    private static final Logger LOGGER = LogManager.getLogger();

    public static Flux<SyndEntry> readLatestEntries(RssSeenEntries entries, RssSource source) {
        var httpClient = HttpClient.newHttpClient();

        return Flux.interval(Duration.ZERO, source.pollFrequency())
            // Drop if we're still working
            .onBackpressureDrop()
            // Make the request (async)
            .concatMap(__ -> {
                LOGGER.info(() -> "Checking for items from " + source.id());
                return Mono.fromFuture(httpClient.sendAsync(
                    HttpRequest.newBuilder(source.uri())
                        .GET()
                        .header("User-Agent", "rss-dokusha")
                        .build(),
                    HttpResponse.BodyHandlers.ofInputStream()
                )).materialize();
            })
            // Map to feed entries
            .concatMap(responseSignal ->
                Flux.<SyndEntry>create(sink -> {
                    if (responseSignal.isOnError()) {
                        LOGGER.warn(() -> "Failed to check items from " + source.id(), responseSignal.getThrowable());
                        sink.complete();
                        return;
                    }
                    if (responseSignal.isOnComplete()) {
                        sink.complete();
                        return;
                    }
                    if (!responseSignal.hasValue()) {
                        return;
                    }
                    SyndFeed feed;
                    try (var body = responseSignal.get().body()) {
                        feed = new SyndFeedInput().build(new XmlReader(body));
                    } catch (IOException | FeedException e) {
                        LOGGER.warn(() -> "Failed to check items from " + source.id(), e);
                        sink.complete();
                        return;
                    }
                    try {
                        for (SyndEntry entry : feed.getEntries()) {
                            sink.next(entry);
                        }
                        sink.complete();
                    } catch (Throwable t) {
                        sink.error(t);
                    }
                }).subscribeOn(Schedulers.boundedElastic())
            )
            // Filter to just new entries
            .filter(e -> entries.addIfUnseen(e.getUri()));
    }

    private RssReader() {
    }
}
