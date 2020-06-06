import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Optional;
import java.util.logging.Logger;

public class ChatServer {
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());

    private final ByteBuffer buffer = ByteBuffer.allocate(Client.BUFFER_SIZE);

    public static void main(String[] args) throws Exception {
        ChatServer server = new ChatServer();
        server.run();
    }

    private void run() throws IOException {
        Selector selector = Selector.open();

        ServerSocketChannel serverSocket = ServerSocketChannel.open();
        serverSocket.bind(new InetSocketAddress("localhost", 10000));
        serverSocket.configureBlocking(false);
        serverSocket.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();

                if (!key.isValid())
                    continue;

                if (key.isAcceptable()) {
                    accept(selector, serverSocket);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
            }
        }
    }

    private void accept(Selector selector, ServerSocketChannel serverSocket) throws IOException {
        SocketChannel clientChannel = serverSocket.accept();
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE, new Client());

        logger.info(String.format("Accepted connection from %s", clientChannel.getRemoteAddress().toString()));
    }

    private void read(SelectionKey readKey) {
        getClient(readKey).ifPresent(srcClient -> {
            SocketChannel channel = getNonBlockingChannel(readKey);
            try {
                srcClient.read(getNonBlockingChannel(readKey));
            } catch (IOException e) {
                logger.warning(String.format("Error reading from socket %s: %s. The offending channel will be closed.", channel.socket(), e.getMessage()));
                shutdown(readKey);
            }

            if (!srcClient.isActive()) {
                // client disconnected -> shutdown channel
                logger.info(String.format("Disconnect detected for socket %s. The channel will be closed.", channel.socket()));
                shutdown(readKey);
                return;
            }

            broadcastMessages(srcClient, readKey);
        });
    }

    private void write(SelectionKey key) {
        getClient(key).ifPresent(c -> {
            SocketChannel channel = getNonBlockingChannel(key);
            try {
                c.write(channel);
            } catch (IOException e) {
                logger.warning(String.format("Error writing to socket %s: %s. The offending channel will be closed.", channel.socket(), e.getMessage()));
                shutdown(key);
            }
        });
    }

    private void broadcastMessages(Client srcClient, SelectionKey readKey){
        buffer.clear();
        srcClient.consume(buffer);
        buffer.flip();

        if(!buffer.hasRemaining()){
            return;
        }

        for (SelectionKey k : readKey.selector().keys()) {
            if (k != readKey) {
                getClient(k).ifPresent(dstClient -> {
                    dstClient.send(buffer);
                    buffer.rewind();
                });
            }
        }
    }

    private SocketChannel getNonBlockingChannel(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        try {
            channel.configureBlocking(false);
            return channel;
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to set non-blocking mode for %s", channel), e);
        }
    }

    private Optional<Client> getClient(SelectionKey key) {
        if (key.attachment() instanceof Client) {
            return Optional.of((Client) key.attachment());
        }

        return Optional.empty();
    }

    private void shutdown(SelectionKey key){
        SocketChannel channel = (SocketChannel)key.channel();
        key.cancel();

        try {
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(String.format("An error occurred trying to close socket channel for %s: %s", channel.socket(), e.getMessage()), e);
        }
    }
}
