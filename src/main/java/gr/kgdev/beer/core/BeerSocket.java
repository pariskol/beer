package gr.kgdev.beer.core;

import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

@WebSocket
public class BeerSocket {
	private final Consumer<Session> onConnect;
	private final BiConsumer<Session, String> onMessage;
	private final Consumer<Session> onClose;

	public BeerSocket(Consumer<Session> onConnect, BiConsumer<Session, String> onMessage, Consumer<Session> onClose) {
		this.onConnect = onConnect;
		this.onMessage = onMessage;
		this.onClose = onClose;
	}

	@OnWebSocketConnect
	public void handleConnect(Session session) {
		if (onConnect != null)
			onConnect.accept(session);
	}

	@OnWebSocketMessage
	public void handleMessage(Session session, String message) {
		if (onMessage != null)
			onMessage.accept(session, message);
	}

	@OnWebSocketClose
	public void handleClose(Session session, int statusCode, String reason) {
		if (onClose != null)
			onClose.accept(session);
	}
}
