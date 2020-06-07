package io.klinamen.niochat.unit;

import io.klinamen.niochat.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ChatRoomTest {
    @Test
    public void should_send_to_all_recipients_when_broadcast() throws IOException {
        ChatSessionImpl source = new ChatSessionImpl();

        List<ChatSession> recipients = new ArrayList<>();
        recipients.add(new ChatSessionImpl());
        recipients.add(new ChatSessionImpl());

        ChatRoom room = new ChatRoom();
        recipients.forEach(room::join);

        MockReadChannel inChannel = new MockReadChannel();
        MockWriteChannel outChannel = new MockWriteChannel();

        source.read(inChannel);

        room.broadcast(source);

        for (ChatSession recipient : recipients) {
            recipient.write(outChannel);
            Assert.assertArrayEquals(inChannel.getLastReadData(), outChannel.getLastWritten());
        }
    }

    @Test
    public void should_not_echo_when_broadcast() throws IOException {
        ChatSessionImpl source = new ChatSessionImpl();

        ChatRoom room = new ChatRoom();
        room.join(source);

        MockReadChannel inChannel = new MockReadChannel();
        MockWriteChannel outChannel = new MockWriteChannel();

        source.read(inChannel);

        room.broadcast(source);

        int written = source.write(outChannel);
        Assert.assertEquals(0, written);
    }
}