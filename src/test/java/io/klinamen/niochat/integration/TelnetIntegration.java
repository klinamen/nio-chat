package io.klinamen.niochat.integration;

import io.klinamen.niochat.ChatServer;
import org.apache.commons.net.telnet.TelnetClient;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class TelnetIntegration {
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

    @Test()
    public void should_broadcast_to_all() throws Exception {
        List<TelnetClient> recipients = new ArrayList<>();
        recipients.add(createConnectedClient());
        recipients.add(createConnectedClient());

        // send message
        TelnetClient sender = createConnectedClient();
        byte[] sent = "Hello!\r\n".getBytes(StandardCharsets.US_ASCII);
        sender.getOutputStream().write(sent);
        sender.getOutputStream().flush();

        for (TelnetClient recipient : recipients) {
            byte[] received = new byte[sent.length];
            recipient.getInputStream().read(received);
            recipient.disconnect();

            Assert.assertArrayEquals(sent, received);
        }

        sender.disconnect();
    }

    private TelnetClient createConnectedClient() throws IOException {
        TelnetClient c = new TelnetClient();
        c.connect(DEFAULT_BINDING_IF, DEFAULT_PORT);
        return c;
    }
}
