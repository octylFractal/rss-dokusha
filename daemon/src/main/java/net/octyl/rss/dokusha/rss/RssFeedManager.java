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

import java.util.Map;

import net.octyl.rss.dokusha.config.RssSource;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class RssFeedManager {
    public static Flux<RssFeedItem> readLatestEntries(RssSeenEntriesCreator seenEntriesCreator,
                                                      Map<String, RssSource> rssSources) {
        // For each source
        return Flux.fromIterable(rssSources.values())
            .flatMap(source ->
                // Open database
                Mono.fromCallable(() -> seenEntriesCreator.create(source))
                    // And convert that into the latest entries stream
                    .flatMapMany(db ->
                        RssReader.readLatestEntries(db, source).doOnTerminate(db::close)
                    )
                    .map(entry -> new RssFeedItem(source, entry))
            );
    }
}
