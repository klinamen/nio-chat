package io.klinamen.niochat;

import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

public class MockWriteChannel implements WritableByteChannel {
    private final int size;

    private byte[] lastWritten;

    public MockWriteChannel() {
        this.size = Integer.MAX_VALUE;
    }

    public MockWriteChannel(int size) {
        this.size = size;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() {

    }

    @Override
    public int write(ByteBuffer byteBuffer) {
        int written = Math.min(byteBuffer.remaining(), size);
        lastWritten = new byte[written];
        byteBuffer.get(lastWritten);
        return written;
    }

    public byte[] getLastWritten() {
        return lastWritten;
    }
}
