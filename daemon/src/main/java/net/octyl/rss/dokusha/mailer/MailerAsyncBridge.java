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

import org.jetbrains.annotations.Nullable;
import org.simplejavamail.api.mailer.AsyncResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class MailerAsyncBridge {
    public static Mono<Void> bridgeAsyncResponse(@Nullable AsyncResponse response) {
        if (response == null) {
            return Mono.empty();
        }
        return Mono.create(sink -> {
            response.onException(sink::error);
            response.onSuccess(sink::success);
        });
    }

    private MailerAsyncBridge() {
    }
}
