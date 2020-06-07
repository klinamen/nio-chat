package io.klinamen.niochat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.logging.Logger;

public class ChatSessionImpl implements ChatSession {
    public static final int BUFFER_SIZE = 1024;

    private static final Logger logger = Logger.getLogger(ChatSessionImpl.class.getName());

    private final ByteBuffer outBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer inBuffer = ByteBuffer.allocate(BUFFER_SIZE);

    private boolean active = true;

    @Override
    public int read(ReadableByteChannel channel) throws IOException {
        int read = channel.read(inBuffer);
        if (read == -1) {
            active = false;
        }

        return read;
    }

    @Override
    public int write(WritableByteChannel channel) throws IOException {
        outBuffer.flip();
        int written = channel.write(outBuffer);
        outBuffer.compact();
        return written;
    }

    @Override
    public int send(ByteBuffer buf) {
        if (outBuffer.remaining() < buf.remaining()) {
            // drops incoming data to prevent outbound buffer overflow
            logger.warning(String.format("Cannot accept %s bytes for sending without overflowing outbound buffer. Data will be lost.", buf.remaining()));
            return 0;
        }
        int length = buf.remaining();
        outBuffer.put(buf);
        return length;
    }

    @Override
    public int consume(ByteBuffer buf) {
        int consumed = 0;
        inBuffer.flip();
        while (inBuffer.hasRemaining() && buf.hasRemaining()) {
            buf.put(inBuffer.get());
            consumed++;
        }
        inBuffer.compact();
        return consumed;
    }

    @Override
    public boolean isActive() {
        return active;
    }
}
