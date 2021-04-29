package net.octyl.rss.dokusha.comm;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import com.google.common.io.Closer;
import com.google.protobuf.InvalidProtocolBufferException;
import net.octyl.rss.dokusha.comm.protos.ConfigUpdated;
import net.octyl.rss.dokusha.comm.protos.IpcMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.test.StepVerifier;

public class UnixSocketDokushaIpcTest {
    private SocketChannel serverSocket;
    private UnixSocketDokushaIpc server;
    private UnixSocketDokushaIpc client;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        var socketFile = UnixDomainSocketAddress.of(tempDir.resolve("test.ipc"));
        var serverSocketTemp = ServerSocketChannel.open(StandardProtocolFamily.UNIX)
            .bind(socketFile);
        var serverSocketFtr = CompletableFuture.supplyAsync(() -> {
            try {
                return serverSocketTemp.accept();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        var clientSocket = SocketChannel.open(socketFile);
        server = new UnixSocketDokushaIpc(this.serverSocket = serverSocketFtr.join());
        client = new UnixSocketDokushaIpc(clientSocket);
    }

    @AfterEach
    void tearDown() throws IOException {
        var closer = Closer.create();
        closer.register(server);
        closer.register(client);
        closer.close();
    }

    @Test
    void cycleConfigUpdated() throws IOException {
        var message = IpcMessage.newBuilder().setConfigUpdated(ConfigUpdated.getDefaultInstance()).build();
        server.sendMessage(message);
        StepVerifier.create(client.messages().doOnNext(__ -> {
            try {
                server.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }))
            .expectNext(message)
            .expectComplete()
            .verify(Duration.ofSeconds(2));
    }

    @Test
    void junkInSocket() throws IOException {
        serverSocket.write(ByteBuffer.wrap(new byte[] {0x01, (byte) 0xBE}));
        StepVerifier.create(client.messages().doOnNext(__ -> {
            try {
                server.close();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }))
            .expectError(InvalidProtocolBufferException.class)
            .verify(Duration.ofSeconds(2));
    }
}
