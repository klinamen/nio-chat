package io.klinamen.niochat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class ChatServer implements Runnable {
    private static final Logger logger = Logger.getLogger(ChatServer.class.getName());

    private final BufferedPipe pipe = new BufferedPipe(ChatSessionImpl.BUFFER_SIZE);
    private final String bindingHostname;
    private final int bindingPort;

    private Selector selector;
    private ServerSocketChannel serverSocket;

    private boolean isClosing = false;

    public ChatServer(String bindingHostname, int bindingPort) {
        this.bindingHostname = bindingHostname;
        this.bindingPort = bindingPort;
    }

    public void run() {
        init();
        mainLoop();
        cleanUp();
    }

    private void init() {
        try {
            serverSocket = ServerSocketChannel.open();
            serverSocket.bind(new InetSocketAddress(bindingHostname, bindingPort));
            serverSocket.configureBlocking(false);

            selector = Selector.open();
            serverSocket.register(selector, SelectionKey.OP_ACCEPT);

            logger.info(String.format("Server ready to accept connections on %s.", serverSocket.socket()));
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error initializing server socket: %s", e.getMessage()), e);
        }
    }

    private void mainLoop() {
        while (!isClosing) {
            try {
                selector.select();
            } catch (IOException e) {
                throw new RuntimeException(String.format("Error selecting socket: %s", e.getMessage()), e);
            }

            Iterator<SelectionKey> iter = selector.selectedKeys().iterator();
            while (iter.hasNext()) {
                SelectionKey key = iter.next();
                iter.remove();

                if (!key.isValid())
                    continue;

                if (key.isAcceptable()) {
                    accept(key);
                } else if (key.isReadable()) {
                    read(key);
                } else if (key.isWritable()) {
                    write(key);
                }
            }
        }
    }

    private void cleanUp() {
        try {
            selector.close();
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(String.format("Error occurred while shutting down: %s", e.getMessage()), e);
        }
    }

    private void accept(SelectionKey key) {
        try {
            SocketChannel clientChannel = serverSocket.accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(key.selector(), SelectionKey.OP_READ | SelectionKey.OP_WRITE, new ChatSessionImpl());
            logger.info(String.format("Accepted connection from %s", clientChannel.getRemoteAddress().toString()));
        } catch (IOException e) {
            logger.severe(String.format("Error accepting connection: %s. Channel will be closed.", e.getMessage()));
            cancel(key);
        }
    }

    private void read(SelectionKey readKey) {
        getClient(readKey).ifPresent(srcClientSession -> {
            SocketChannel channel = getNonBlockingChannel(readKey);
            try {
                srcClientSession.read(getNonBlockingChannel(readKey));
            } catch (IOException e) {
                logger.warning(String.format("Error reading from socket %s: %s. The offending channel will be closed.", channel.socket(), e.getMessage()));
                cancel(readKey);
            }

            if (!srcClientSession.isActive()) {
                // client disconnected -> cancel channel
                logger.info(String.format("Disconnect detected for socket %s. The channel will be closed.", channel.socket()));
                cancel(readKey);
                return;
            }

            broadcast(srcClientSession, readKey);
        });
    }

    private void broadcast(ChatSession source, SelectionKey sourceKey) {
        List<ChatSession> broadcastRecipients = selector.keys().stream()
                .filter(k -> k != sourceKey)
                .map(this::getClient)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        pipe.multicast(source, broadcastRecipients);
    }

    private void write(SelectionKey key) {
        getClient(key).ifPresent(c -> {
            SocketChannel channel = getNonBlockingChannel(key);
            try {
                c.write(channel);
            } catch (IOException e) {
                logger.warning(String.format("Error writing to socket %s: %s. The offending channel will be closed.", channel.socket(), e.getMessage()));
                cancel(key);
            }
        });
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

    private Optional<ChatSession> getClient(SelectionKey key) {
        if (key.attachment() instanceof ChatSessionImpl) {
            return Optional.of((ChatSession) key.attachment());
        }

        return Optional.empty();
    }

    private void cancel(SelectionKey key) {
        SocketChannel channel = (SocketChannel) key.channel();
        key.cancel();

        try {
            channel.close();
        } catch (IOException e) {
            throw new RuntimeException(String.format("An error occurred trying to close socket channel for %s: %s", channel.socket(), e.getMessage()), e);
        }
    }

    public void shutdown() {
        isClosing = true;
    }
}
