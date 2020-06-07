package io.klinamen.niochat.unit;

import io.klinamen.niochat.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class ChatSessionImplTest {
    @Test
    public void should_buffer_input_when_read_from_channel() throws IOException {
        MockReadChannel inChannel = new MockReadChannel();
        ChatSession session = new ChatSessionImpl();

        int read = session.read(inChannel);

        ByteBuffer buffer = ByteBuffer.allocate(ChatSessionImpl.BUFFER_SIZE);
        session.consume(buffer);

        byte[] bufferedData = new byte[buffer.position()];
        buffer.flip();
        buffer.get(bufferedData);

        Assert.assertArrayEquals(Utils.buildFilledArray(MockReadChannel.FILL_VALUE, read), bufferedData);
    }

    @Test
    public void should_not_change_active_state_when_read_from_channel() throws IOException {
        MockReadChannel inChannel = new MockReadChannel();
        ChatSession session = new ChatSessionImpl();
        session.read(inChannel);
        Assert.assertTrue(session.isActive());
    }

    @Test
    public void should_buffer_output_when_send() throws IOException {
        byte[] sentData = Utils.buildFilledArray(MockReadChannel.FILL_VALUE, 64);

        ByteBuffer buffer = ByteBuffer.allocate(256);
        buffer.put(sentData);

        buffer.flip();

        ChatSession session = new ChatSessionImpl();
        session.send(buffer);

        MockWriteChannel outChannel = new MockWriteChannel();
        session.write(outChannel);

        Assert.assertArrayEquals(sentData, outChannel.getLastWritten());
    }

    @Test
    public void should_drop_data_when_send_overflows_output_buffer() throws IOException {
        byte[] sentData = Utils.buildFilledArray(MockReadChannel.FILL_VALUE, ChatSessionImpl.BUFFER_SIZE * 2);

        ByteBuffer buffer = ByteBuffer.allocate(ChatSessionImpl.BUFFER_SIZE * 2);
        buffer.put(sentData);

        buffer.flip();

        ChatSession session = new ChatSessionImpl();
        session.send(buffer);

        MockWriteChannel outChannel = new MockWriteChannel();
        int written = session.write(outChannel);

        Assert.assertEquals(0, written);
    }

    @Test
    public void should_become_inactive_when_channel_reaches_end() throws IOException {
        FailingReadChannel inChannel = new FailingReadChannel();
        ChatSession session = new ChatSessionImpl();

        session.read(inChannel);
        Assert.assertFalse(session.isActive());
    }

    @Test
    public void should_not_throw_when_consuming_empty_input_buffer() {
        ChatSession session = new ChatSessionImpl();
        ByteBuffer buffer = ByteBuffer.allocate(256);
        session.consume(buffer);
        Assert.assertEquals(0, buffer.position());
    }

    @Test
    public void test_partial_writes() throws IOException {
        byte[] sentData = Utils.buildFilledArray(MockReadChannel.FILL_VALUE, ChatSessionImpl.BUFFER_SIZE);

        ByteBuffer buffer = ByteBuffer.allocate(ChatSessionImpl.BUFFER_SIZE);
        buffer.put(sentData);

        buffer.flip();

        ChatSession session = new ChatSessionImpl();
        session.send(buffer);

        final int writeCycles = 4;
        MockWriteChannel outChannel = new MockWriteChannel(ChatSessionImpl.BUFFER_SIZE / writeCycles);

        for (int i = 0; i < writeCycles; i++) {
            int written = session.write(outChannel);
            Assert.assertEquals(sentData.length / writeCycles, written);
        }

        int written = session.write(outChannel);
        Assert.assertEquals(0, written);
    }
}

