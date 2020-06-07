package io.klinamen.niochat;

import java.nio.ByteBuffer;

/**
 * A buffered channel to move data between chat sessions.
 */
public class BufferedPipe {
    public static final int DEFAULT_BUFFER_SIZE = 1024;

    private final ByteBuffer buffer;

    public BufferedPipe() {
        buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
    }

    public BufferedPipe(int bufferSize) {
        buffer = ByteBuffer.allocate(bufferSize);
    }

    /**
     * Consumes data from the output buffer of the source session and sends it to every recipient session.
     * All the data available from the source is consumed prior to send it out to the recipients.
     *
     * @param source     the source session
     * @param recipients the receiving sessions
     */
    public void multicast(ChatSession source, Iterable<ChatSession> recipients) {
        while (true) {
            buffer.clear();
            int consumed = source.consume(buffer);
            buffer.flip();

            if (consumed == 0) {
                break;
            }

            for (ChatSession recipient : recipients) {
                recipient.send(buffer);
                buffer.rewind();
            }
        }
    }
}
