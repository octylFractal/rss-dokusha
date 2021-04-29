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
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.google.common.reflect.TypeToken;

public record EmailSink(
    String id,
    EmailMessageConfig emailMessageConfig,
    SmtpServer smtpServer,
    Map<String, RssSource> sources
) {
    private static final String EMAIL_MESSAGE_NAME = "email-message.properties";
    private static final String SMTP_SERVER_NAME = "smtp-server.properties";

    public static Map<String, EmailSink> loadAll(Path dir) throws IOException {
        if (!Files.isDirectory(dir)) {
            return Map.of();
        }
        var result = new ImmutableMap.Builder<String, EmailSink>();
        try (var files = Files.list(dir)) {
            var fileList = files.filter(Files::isDirectory).toList();
            for (var file : fileList) {
                var emailSink = load(file);
                result.put(emailSink.id(), emailSink);
            }
        }
        return result.build();
    }

    public static EmailSink load(Path dir) throws IOException {
        var id = dir.getFileName().toString();
        var emailMessageConfig = ConfigContent.load(EmailMessageConfig.class, dir.resolve(EMAIL_MESSAGE_NAME));
        var smtpServer = ConfigContent.load(SmtpServer.class, dir.resolve(SMTP_SERVER_NAME));
        var sources = RssSource.loadAll(dir.resolve(Configuration.SOURCES_DIR_NAME));
        return new EmailSink(id, emailMessageConfig, smtpServer, sources);
    }

    public void saveToDirectory(Path dir) throws IOException {
        save(dir.resolve(id));
    }

    private void save(Path dir) throws IOException {
        emailMessageConfig.save(dir.resolve(EMAIL_MESSAGE_NAME));
        smtpServer.save(dir.resolve(SMTP_SERVER_NAME));
        for (RssSource source : sources.values()) {
            source.saveToDirectory(dir.resolve(Configuration.SOURCES_DIR_NAME));
        }
    }
}
