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
import java.net.UnixDomainSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.dataformat.javaprop.JavaPropsMapper;
import com.google.common.base.StandardSystemProperty;
import dev.dirs.ProjectDirectories;

public record Configuration(
    UnixDomainSocketAddress communicationSocket
) implements ConfigContent {
    private static final ProjectDirectories PROJECT_DIRS = ProjectDirectories.from(
        "net.octyl", "", "rss-dokusha"
    );
    public static final String SINKS_DIR_NAME = "email-sinks.d";
    public static final String SOURCES_DIR_NAME = "rss-sources.d";
    public static final String ENTRIES_DB_NAME = "rss-entries";
    public static final Path CONFIG_DIR = Path.of(PROJECT_DIRS.configDir);
    public static final Path DATA_LOCAL_DIR = Path.of(PROJECT_DIRS.dataLocalDir);
    public static final Path CONFIG_FILE = CONFIG_DIR.resolve("config.properties");
    public static final JavaPropsMapper PROPS_MAPPER = new JavaPropsMapper();

    public static Configuration loadOrDefault(Path configFile) throws IOException {
        if (Files.exists(configFile)) {
            return ConfigContent.load(Configuration.class, configFile);
        }
        return new Configuration(
            getDefaultCommunicationSocket()
        );
    }

    private static UnixDomainSocketAddress getDefaultCommunicationSocket() {
        Path path;
        if (PROJECT_DIRS.runtimeDir != null) {
            path = Path.of(PROJECT_DIRS.runtimeDir).resolve("ipc.socket");
        } else {
            path = Path.of(StandardSystemProperty.JAVA_IO_TMPDIR.value())
                .resolve(".dokusha-" + StandardSystemProperty.USER_NAME.value())
                .resolve("ipc.socket");
        }
        return UnixDomainSocketAddress.of(path);
    }

}
