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

package net.octyl.rss.dokusha;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.function.Function;

import net.octyl.rss.dokusha.comm.DokushaCommServer;
import net.octyl.rss.dokusha.config.RssSource;
import net.octyl.rss.dokusha.mailer.MailerManager;
import net.octyl.rss.dokusha.comm.protos.ConfigUpdated;
import net.octyl.rss.dokusha.config.Configuration;
import net.octyl.rss.dokusha.config.EmailSink;
import net.octyl.rss.dokusha.rss.RssSeenEntries;
import net.octyl.rss.dokusha.rss.RssSeenEntriesCreator;
import net.octyl.rss.dokusha.util.MoreFlux;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class DokushaDaemon {
    private static final Logger LOGGER = LogManager.getLogger();

    public static void main(String[] args) throws IOException {
        var configuration = Configuration.loadOrDefault(Configuration.CONFIG_FILE);
        configuration.save(Configuration.CONFIG_FILE);

        var commsServer = new DokushaCommServer(configuration.communicationSocket());
        var commsServerMono = commsServer.serveForever();

        var configUpdateNotifications = Flux.concat(
            Mono.just(ConfigUpdated.getDefaultInstance()),
            commsServer.messages().transform(MoreFlux.instanceOf(ConfigUpdated.class))
        ).cache(1);
       RssSeenEntriesCreator seenEntriesCreator = source -> RssSeenEntries.open(
            Configuration.DATA_LOCAL_DIR.resolve(Configuration.ENTRIES_DB_NAME).resolve(source.id() + ".mapdb")
        );
        var mailerMono = configUpdateNotifications
            .publishOn(Schedulers.boundedElastic())
            .map(__ -> {
                try {
                    return EmailSink.loadAll(
                        Configuration.CONFIG_DIR.resolve(Configuration.SINKS_DIR_NAME)
                    );
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            })
            .publishOn(Schedulers.parallel())
            .switchMap(sinks -> MailerManager.manageEmailSinks(seenEntriesCreator, sinks))
            .then();

        // Await exited services
        try {
            Mono.when(commsServerMono, mailerMono).toFuture().join();
        } catch (Throwable t) {
            LOGGER.fatal("Downstream service(s) exited, program exiting", t);
            System.exit(1);
        }
    }
}
