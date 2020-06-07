package io.klinamen.niochat;

import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a chat room and a broadcast domain for chat sessions.
 * Sessions can join and leave the room, as well as broadcast messages to all the participants.
 */
public class ChatRoom {
    public static final int DEFAULT_BUFFER_SIZE = 1024;

    private final ByteBuffer buffer;
    private final Set<ChatSession> participants = new HashSet<>();

    public ChatRoom() {
        buffer = ByteBuffer.allocate(DEFAULT_BUFFER_SIZE);
    }

    public ChatRoom(int bufferSize) {
        buffer = ByteBuffer.allocate(bufferSize);
    }

    public ChatRoom join(ChatSession session) {
        participants.add(session);
        return this;
    }

    public ChatRoom leave(ChatSession session) {
        participants.remove(session);
        return this;
    }

    /**
     * Consumes data from the output buffer of the source session and sends it to every session in the room, but the source itself.
     * All the data available from the source is consumed prior to send it out to the recipients.
     *
     * @param source the source session
     */
    public void broadcast(ChatSession source) {
        while (true) {
            buffer.clear();
            int consumed = source.consume(buffer);
            buffer.flip();

            if (consumed == 0) {
                break;
            }

            participants.stream()
                    .filter(r -> r != source && r.isActive())
                    .forEach(r -> {
                        r.send(buffer);
                        buffer.rewind();
                    });
        }
    }
}
