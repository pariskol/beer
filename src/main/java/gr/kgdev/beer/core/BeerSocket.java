
package gr.kgdev.beer.core;

import org.eclipse.jetty.websocket.api.annotations.WebSocket;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;

import java.util.function.Consumer;
import java.util.function.BiConsumer;

/**
 * WebSocket handler for Beer server.
 * <p>
 * Uses Jetty WebSocket annotations to handle connection, message, and close
 * events. Accepts functional interfaces for custom event handling.
 */
@WebSocket
public class BeerSocket {
	/** Consumer to handle new WebSocket connections. */
	private final Consumer<Session> onConnect;
	/** BiConsumer to handle incoming WebSocket messages. */
	private final BiConsumer<Session, String> onMessage;
	/** Consumer to handle WebSocket close events. */
	private final Consumer<Session> onClose;

	/**
	 * Constructs a BeerSocket with custom event handlers.
	 *
	 * @param onConnect consumer for connection events
	 * @param onMessage bi-consumer for message events
	 * @param onClose   consumer for close events
	 */
	public BeerSocket(Consumer<Session> onConnect, BiConsumer<Session, String> onMessage, Consumer<Session> onClose) {
		this.onConnect = onConnect;
		this.onMessage = onMessage;
		this.onClose = onClose;
	}

	/**
	 * Handles a new WebSocket connection.
	 *
	 * @param session the WebSocket session
	 */
	@OnWebSocketConnect
	public void handleConnect(Session session) {
		if (onConnect != null)
			onConnect.accept(session);
	}

	/**
	 * Handles an incoming WebSocket message.
	 *
	 * @param session the WebSocket session
	 * @param message the received message
	 */
	@OnWebSocketMessage
	public void handleMessage(Session session, String message) {
		if (onMessage != null)
			onMessage.accept(session, message);
	}

	/**
	 * Handles a WebSocket close event.
	 *
	 * @param session    the WebSocket session
	 * @param statusCode the close status code
	 * @param reason     the reason for closing
	 */
	@OnWebSocketClose
	public void handleClose(Session session, int statusCode, String reason) {
		if (onClose != null)
			onClose.accept(session);
	}
}
