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

public record SmtpServer(
    String host,
    int port,
    String username,
    String password,
    TransportStrategy transportStrategy
) implements ConfigContent {
    public enum TransportStrategy {
        /**
         * Use STARTTLS if offered, but is essentially plaintext even if so.
         */
        OPTIONAL_STARTTLS,
        /**
         * Require STARTTLS and validate certificates.
         */
        REQUIRED_STARTTLS,
        /**
         * Open the connection under TLS, no plaintext communication occurs.
         */
        FULL_TLS
    }
}
