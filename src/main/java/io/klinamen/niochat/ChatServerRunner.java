package io.klinamen.niochat;

public class ChatServerRunner {
    public static final String DEFAULT_BINDING_IF = "localhost";
    public static final int DEFAULT_PORT = 10000;

    private static ChatServer server;

    public static void main(String[] args) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server shutdown started!");
            if (server != null) {
                server.shutdown();
            }
        }));

        server = new ChatServer(DEFAULT_BINDING_IF, DEFAULT_PORT);

        Thread serverThread = new Thread(server);
        serverThread.start();
    }
}
