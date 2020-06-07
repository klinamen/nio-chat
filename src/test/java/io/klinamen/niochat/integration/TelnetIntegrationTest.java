package io.klinamen.niochat.integration;

import io.klinamen.niochat.ChatServer;
import io.klinamen.niochat.ChatSessionImpl;
import io.klinamen.niochat.Utils;
import org.apache.commons.net.telnet.TelnetClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TelnetIntegrationTest {
    public static final String DEFAULT_BINDING_IF = "localhost";
    public static final int DEFAULT_PORT = 10000;

    ChatServer server;
    Thread serverThread;

    @Before
    public void setUp() throws Exception {
        server = new ChatServer(DEFAULT_BINDING_IF, DEFAULT_PORT);
        serverThread = new Thread(server);
        serverThread.start();
        Thread.sleep(1000);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        serverThread.join(2000);
    }

    @Test(timeout = 3000)
    public void should_broadcast_to_all_when_a_message_is_sent() throws Exception {
        List<TelnetClient> recipients = new ArrayList<>();
        recipients.add(createConnectedClient());
        recipients.add(createConnectedClient());

        // send message
        TelnetClient sender = createConnectedClient();
        byte[] sent = "Hello!\r\n".getBytes(StandardCharsets.US_ASCII);
        sender.getOutputStream().write(sent);
        sender.getOutputStream().flush();

        // receive messages
        for (TelnetClient recipient : recipients) {
            byte[] received = new byte[sent.length];
            readUntilBufferIsFull(recipient.getInputStream(), received);
            recipient.disconnect();

            Assert.assertArrayEquals(sent, received);
        }

        sender.disconnect();
    }

    @Test(timeout = 3000)
    public void should_broadcast_when_messages_are_greater_than_buffers() throws Exception {
        List<TelnetClient> recipients = new ArrayList<>();
        recipients.add(createConnectedClient());
        recipients.add(createConnectedClient());

        // send message
        TelnetClient sender = createConnectedClient();

        byte[] sent = Utils.buildFilledArray((byte) 'x', ChatSessionImpl.BUFFER_SIZE * 2);
        sender.getOutputStream().write(sent);
        sender.getOutputStream().flush();

        // receive messages
        for (TelnetClient recipient : recipients) {
            byte[] received = new byte[sent.length];
            readUntilBufferIsFull(recipient.getInputStream(), received);
            recipient.disconnect();

            Assert.assertArrayEquals(sent, received);
        }

        sender.disconnect();
    }

    private void readUntilBufferIsFull(InputStream iStream, byte[] buffer) throws IOException {
        int i = 0;
        while (i < buffer.length) {
            buffer[i++] = (byte) iStream.read();
        }
    }

    private TelnetClient createConnectedClient() throws IOException {
        TelnetClient c = new TelnetClient();
        c.connect(DEFAULT_BINDING_IF, DEFAULT_PORT);
        return c;
    }
}
