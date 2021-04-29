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

package net.octyl.rss.dokusha.comm;

import java.io.Closeable;
import java.io.IOException;

import net.octyl.rss.dokusha.comm.protos.IpcMessage;
import reactor.core.publisher.Flux;

/**
 * IPC interface for rss-dokusha.
 */
public interface DokushaIpc extends Closeable {

    /**
     * Send a message to remote.
     *
     * @param message the message to send
     */
    void sendMessage(IpcMessage message) throws IOException;

    /**
     * {@return the flow of messages from remote}
     *
     * Note that each call will create a new reader, only one should be active so you should
     * {@link Flux#share() share} it!
     */
    Flux<IpcMessage> messages();

}
