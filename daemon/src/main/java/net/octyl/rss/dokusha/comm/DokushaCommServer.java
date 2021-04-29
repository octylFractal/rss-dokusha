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
import java.io.UncheckedIOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import net.octyl.rss.dokusha.comm.protos.IpcMessage;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

public class DokushaCommServer {
    private final ExecutorService servingThreadPool = Executors.newCachedThreadPool(
        new ThreadFactoryBuilder().setNameFormat("dokusha-comms").build()
    );
    private final UnixDomainSocketAddress address;
    private final BlockingQueue<IpcMessage> messageQueue = new ArrayBlockingQueue<>(100);
    private final Flux<IpcMessage> messages = Flux.generate(sink -> {
        try {
            sink.next(messageQueue.take());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            sink.error(e);
        }
    });

    public DokushaCommServer(UnixDomainSocketAddress address) {
        this.address = address;
    }

    public Mono<Void> serveForever() {
        return Mono.fromCallable(() -> ServerSocketChannel.open(StandardProtocolFamily.UNIX))
            .flatMap(s -> Mono.just(s).doOnTerminate(() -> {
                try {
                    s.close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            }))

            var serverSocket = ;
            serverSocket.bind(address);
            while (serverSocket.isOpen()) {
                var child = serverSocket.accept();
                servingThreadPool.submit(() -> {
                    serveChild(child);
                });
            }
            return null;
        })
            .subscribeOn(Schedulers.boundedElastic());
    }

    private void serveChild(SocketChannel child) {
        var ipc = new UnixSocketDokushaIpc(child);
        ipc.messages().subscribe(message -> {
            try {
                messageQueue.put(message);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        });
    }

    public Flux<IpcMessage> messages() {
        return messages;
    }
}
