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
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.common.reflect.TypeToken;

public interface ConfigContent {
    static <C extends ConfigContent> C load(Class<C> type, Path file) throws IOException {
        return load(TypeToken.of(type), file);
    }

    static <C extends ConfigContent> C load(TypeToken<C> type, Path file) throws IOException {
        try (var reader = Files.newBufferedReader(file)) {
            return Configuration.PROPS_MAPPER.readValue(reader, Configuration.PROPS_MAPPER.constructType(type.getType()));
        }
    }

    default void save(Path file) throws IOException {
        Files.createDirectories(file.getParent());
        try (var writer = Files.newBufferedWriter(file)) {
            Configuration.PROPS_MAPPER.writeValue(writer, this);
        }
    }
}
