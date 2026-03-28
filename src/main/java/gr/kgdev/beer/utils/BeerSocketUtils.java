package gr.kgdev.beer.utils;

import org.eclipse.jetty.websocket.api.Session;

/**
 * Utility class providing helper methods for WebSocket communication.
 * <p>
 * This class simplifies sending objects over a Jetty WebSocket session
 * by handling JSON serialization and message transmission.
 * <p>
 * All methods are static and intended for server-side WebSocket usage.
 */
public class BeerSocketUtils {

	/**
     * Sends an object to a WebSocket client through the given session.
     * <p>
     * The object is serialized to JSON before being sent as a text message.
     *
     * @param session the active WebSocket {@link Session} used to send the message
     * @param object the object to be serialized and sent
     * @throws RuntimeException if serialization or message transmission fails
     */
	public static void send(Session session, Object object) {
        try {
			session.sendText(BeerUtils.json(object), null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
	}
}
