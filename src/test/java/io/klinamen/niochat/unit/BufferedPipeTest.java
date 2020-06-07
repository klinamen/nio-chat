package io.klinamen.niochat.unit;

import io.klinamen.niochat.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BufferedPipeTest {
    @Test
    public void should_send_to_all_recipients_when_multicast() throws IOException {
        ChatSessionImpl source = new ChatSessionImpl();

        List<ChatSession> recipients = new ArrayList<>();
        recipients.add(new ChatSessionImpl());
        recipients.add(new ChatSessionImpl());

        MockReadChannel inChannel = new MockReadChannel();
        MockWriteChannel outChannel = new MockWriteChannel();

        source.read(inChannel);

        BufferedPipe pipe = new BufferedPipe();
        pipe.multicast(source, recipients);

        for (ChatSession recipient : recipients) {
            recipient.write(outChannel);
            Assert.assertArrayEquals(inChannel.getLastReadData(), outChannel.getLastWritten());
        }
    }
}