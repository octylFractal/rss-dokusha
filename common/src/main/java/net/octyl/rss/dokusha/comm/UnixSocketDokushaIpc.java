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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.SocketChannel;

import net.octyl.rss.dokusha.comm.protos.IpcMessage;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

public class UnixSocketDokushaIpc implements DokushaIpc {

    private final SocketChannel channel;
    private final OutputStream wrapper;

    public UnixSocketDokushaIpc(SocketChannel channel) {
        this.channel = channel;
        this.wrapper = Channels.newOutputStream(channel);
    }

    @Override
    public void sendMessage(IpcMessage message) throws IOException {
        message.writeDelimitedTo(wrapper);
    }

    @Override
    public Flux<IpcMessage> messages() {
        var inputStream = Channels.newInputStream(channel);
        return Flux.<IpcMessage>generate(sink -> {
            try {
                var message = IpcMessage.parseDelimitedFrom(inputStream);
                if (message == null) {
                    sink.complete();
                    return;
                }
                sink.next(message);
            } catch (IOException e) {
                sink.error(e);
            }
        })
            .subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public void close() throws IOException {
        wrapper.close();
        channel.close();
    }
}
