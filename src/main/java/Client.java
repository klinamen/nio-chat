import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

public class Client {
    public static final int BUFFER_SIZE = 1024;

    private final ByteBuffer outBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final ByteBuffer inBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private int latestMessageOffset = -1;

    private boolean active = true;

    public int read(ReadableByteChannel channel) throws IOException {
        int read = channel.read(inBuffer);
        if (read == -1) {
            active = false;
        } else if (read > 0) {
            byte[] data = inBuffer.array();
            int pos = inBuffer.position() + inBuffer.arrayOffset();
            for (int i = 0; i < pos; i++) {
                scan(data[i]);
            }
        }

        return read;
    }

    private void scan(byte b) {
        if (isEndOfLine(b)) {
            latestMessageOffset = inBuffer.position();
        }
    }

    public int write(WritableByteChannel channel) throws IOException {
        outBuffer.flip();
        int written = channel.write(outBuffer);
        outBuffer.compact();
        return written;
    }

    public void send(ByteBuffer buf) {
        outBuffer.put(buf);
    }

    public void consume(ByteBuffer buf) {
        if (latestMessageOffset < 0) {
            return;
        }

        int oldLimit = inBuffer.limit();
        inBuffer.flip().limit(latestMessageOffset);

        buf.put(inBuffer);
        latestMessageOffset = -1;

        inBuffer.limit(oldLimit).compact();
    }

    private boolean isEndOfLine(byte c) {
        return c == '\n' || c == '\r';
    }

    public boolean isActive() {
        return active;
    }
}
