package io.klinamen.niochat;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class MockReadChannel implements ReadableByteChannel {
    public static final byte FILL_VALUE = (byte) '*';

    private final double fillPerc;

    private byte[] lastReadData;

    public MockReadChannel() {
        this.fillPerc = 1;
    }

    public MockReadChannel(double fillPerc) {
        this.fillPerc = fillPerc;
    }

    @Override
    public int read(ByteBuffer byteBuffer) {
        int read = (int) (fillPerc * byteBuffer.remaining());
        lastReadData = Utils.buildFilledArray(FILL_VALUE, read);
        byteBuffer.put(lastReadData);
        return read;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void close() {

    }

    public byte[] getLastReadData() {
        return lastReadData;
    }
}
