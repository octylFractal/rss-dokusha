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

package net.octyl.rss.dokusha.config;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Pattern;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;

public record RssSource(
    String id,
    URI uri,
    Duration pollFrequency
) {
    private static final String RSS_SOURCE_EXTENSION = "properties";

    private record RssSourceSerialized(URI uri, long pollFrequencySeconds) implements ConfigContent {
    }

    public static Map<String, RssSource> loadAll(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return Map.of();
        }
        var result = new ImmutableMap.Builder<String, RssSource>();
        try (var files = Files.list(dir)) {
            var fileList = files.toList();
            for (var file : fileList) {
                var rssSource = load(file);
                result.put(rssSource.id(), rssSource);
            }
        }
        return result.build();
    }

    private static RssSource load(Path file) throws IOException {
        RssSourceSerialized serializedForm = ConfigContent.load(new TypeToken<>() {}, file);
        return new RssSource(
            file.getFileName().toString()
                .replaceFirst("\\." + Pattern.quote(RSS_SOURCE_EXTENSION) + "$", ""),
            serializedForm.uri(),
            Duration.ofSeconds(serializedForm.pollFrequencySeconds())
        );
    }

    public void saveToDirectory(Path dir) throws IOException {
        save(dir.resolve(id() + "." + RSS_SOURCE_EXTENSION));
    }

    private void save(Path file) throws IOException {
        new RssSourceSerialized(uri(), pollFrequency().toSeconds()).save(file);
    }
}
