package io.klinamen.niochat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Represents a chat session
 */
public interface ChatSession {
    /**
     * Reads from a channel into the internal input buffer and returns the number of bytes read.
     * If the end-of-stream is reached, the session becomes inactive.
     *
     * @param channel the input channel to read from
     * @return the number of bytes read from the channel
     * @throws IOException if I/O errors occur reading from the channel
     */
    int read(ReadableByteChannel channel) throws IOException;

    /**
     * Flushes the internal output buffer to an output channel and returns the number of bytes written.
     *
     * @param channel the output channel
     * @return the number of bytes written
     * @throws IOException if I/O errors occur writing to the channel
     */
    int write(WritableByteChannel channel) throws IOException;

    /**
     * Accepts data for sending and stores it in the internal output buffer and returns the number of bytes accepted.
     * If incoming data does not overflows the buffer, then it is accepted. Otherwise, incoming data is dropped.
     * Incoming data is accepted or dropped entirely without truncation.
     *
     * @param buf an input buffer to read incoming data from
     * @return the number of bytes accepted
     */
    int send(ByteBuffer buf);

    /**
     * Consumes data from the internal input buffer and returns the number of bytes consumed.
     * Data is copied from the input buffer to the given buffer until its remaining capacity is exhausted.
     *
     * @param buf a buffer for storing consumed data
     * @return the number of bytes consumed
     */
    int consume(ByteBuffer buf);

    /**
     * Returns the activity status of the session.
     *
     * @return the activity status of the session
     */
    boolean isActive();
}
