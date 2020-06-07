package io.klinamen.niochat;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class FailingReadChannel implements ReadableByteChannel {

    @Override
    public int read(ByteBuffer byteBuffer) {
        return -1;
    }

    @Override
    public boolean isOpen() {
        return false;
    }

    @Override
    public void close() {

    }
}
