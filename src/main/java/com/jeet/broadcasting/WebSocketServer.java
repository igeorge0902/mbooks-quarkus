package com.jeet.broadcasting;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

@ServerEndpoint("/ws")
public class WebSocketServer {
    private static final CopyOnWriteArraySet<Session> sessions = new CopyOnWriteArraySet<>();

    @OnOpen
    public void onOpen(Session session) {
        session.setMaxIdleTimeout(0); // Disable idle timeout; rely on client pings
        sessions.add(session);
        System.out.println("New connection: " + session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        System.out.println("Connection closed: " + session.getId());
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        sessions.remove(session);
        System.err.println("WebSocket error for session " + session.getId() + ": " + throwable.getMessage());
    }

    @OnMessage
    public void onPong(PongMessage pong, Session session) {
        // Client ping/pong keepalive — no action needed, just keeps the connection alive
    }

    public static void broadcastMessage(String message) {
        for (Session session : sessions) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                sessions.remove(session);
                System.err.println("Removed dead session " + session.getId() + ": " + e.getMessage());
            }
        }
    }
}
