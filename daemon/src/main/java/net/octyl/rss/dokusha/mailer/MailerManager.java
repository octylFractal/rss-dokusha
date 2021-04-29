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

package net.octyl.rss.dokusha.mailer;

import java.time.Duration;
import java.util.Map;

import com.google.common.html.HtmlEscapers;
import net.octyl.rss.dokusha.config.EmailMessageConfig;
import net.octyl.rss.dokusha.config.EmailSink;
import net.octyl.rss.dokusha.config.EmailWithName;
import net.octyl.rss.dokusha.config.SmtpServer;
import net.octyl.rss.dokusha.rss.RssFeedItem;
import net.octyl.rss.dokusha.rss.RssFeedManager;
import net.octyl.rss.dokusha.rss.RssSeenEntriesCreator;
import net.octyl.rss.dokusha.util.MoreFlux;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class MailerManager {
    private static final Logger LOGGER = LogManager.getLogger();

    public static Mono<Void> manageEmailSinks(RssSeenEntriesCreator seenEntriesCreator,
                                              Map<String, EmailSink> emailSinks) {
        // For each sink
        return Flux.fromIterable(emailSinks.values())
            .flatMap(sink -> {
                // Open mailer
                var mailer = MailerBuilder
                    .withSMTPServer(
                        sink.smtpServer().host(), sink.smtpServer().port(),
                        sink.smtpServer().username(), sink.smtpServer().password()
                    )
                    .withTransportStrategy(convertTransportStrategy(sink.smtpServer().transportStrategy()))
                    .buildMailer();
                // Validate connection
                return MailerAsyncBridge.bridgeAsyncResponse(mailer.testConnection(true))
                    .then(Mono.defer(() -> createMailerFlux(seenEntriesCreator, mailer, sink)))
                    .doOnTerminate(() ->
                        Mono.fromCallable(mailer.shutdownConnectionPool()::get)
                            .subscribeOn(Schedulers.boundedElastic())
                            .doOnError(e -> LOGGER.warn("Error shutting down mailer pool", e))
                            .subscribe()
                    );
            })
            .then();
    }

    private static Mono<Void> createMailerFlux(RssSeenEntriesCreator seenEntriesCreator,
                                               Mailer mailer, EmailSink sink) {
        record EmailWithSourceId(Email email, String sourceId) {}
        // Listen to sources
        return RssFeedManager.readLatestEntries(seenEntriesCreator, sink.sources())
            // Insert a 1s interval in-between any possible emissions
            .transform(MoreFlux.throttle(Duration.ofSeconds(1)))
            .doOnNext(e -> LOGGER.info(() ->
                "Sending email to sink " +
                    "'" + sink.id() + "'" +
                    " for item from " +
                    "'" + e.source().id() + "'" +
                    ": " + e.entry().getUri()
            ))
            .map(e -> new EmailWithSourceId(convertFeedItemToEmail(e, sink.emailMessageConfig()), e.source().id()))
            .flatMap(emailWithSourceId ->
                MailerAsyncBridge.bridgeAsyncResponse(
                    mailer.sendMail(emailWithSourceId.email(), true)
                )
                    // Recover on failure
                    .onErrorResume(e -> {
                        LOGGER.warn(
                            () -> "Failed to send email to " + sink.id() + " for event from "
                                + emailWithSourceId.sourceId(),
                            e
                        );
                        return Mono.empty();
                    }))
            .then();
    }

    private static TransportStrategy convertTransportStrategy(SmtpServer.TransportStrategy transportStrategy) {
        return switch (transportStrategy) {
            case OPTIONAL_STARTTLS -> TransportStrategy.SMTP;
            case REQUIRED_STARTTLS -> TransportStrategy.SMTP_TLS;
            case FULL_TLS -> TransportStrategy.SMTPS;
        };
    }

    public static Email convertFeedItemToEmail(RssFeedItem item, EmailMessageConfig config) {
        var messageBuilder = EmailBuilder.startingBlank();
        for (EmailWithName destination : config.destinations()) {
            messageBuilder.bcc(destination.name(), destination.email());
        }
        messageBuilder.from(config.from().name(), config.from().email());
        messageBuilder.withSubject("[RSS-Dokusha/%s] %s".formatted(
            item.source().id(), item.entry().getTitle()
        ));
        var desc = item.entry().getDescription();
        String html = "text/html".equals(desc.getType())
            ? desc.getValue()
            : HtmlEscapers.htmlEscaper().escape(desc.getValue());
        messageBuilder.withHTMLText(
            """
            %s

            <p>
            (from %s)
            </p>
            """.formatted(html, item.entry().getLink())
        );
        config.sMimeConfig().ifPresent(sMime ->
            messageBuilder.encryptWithSmime(sMime.pemFile().toFile())
        );
        config.dkimConfig().ifPresent(dkim ->
            messageBuilder.signWithDomainKey(
                dkim.privateKeyFile().toFile(),
                dkim.signingDomain(),
                dkim.dkimSelector()
            )
        );
        return messageBuilder.buildEmail();
    }
}
