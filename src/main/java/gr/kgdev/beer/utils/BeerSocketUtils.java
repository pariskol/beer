package gr.kgdev.beer.utils;

import org.eclipse.jetty.websocket.api.Session;

public class BeerSocketUtils {

	public static void send(Session session, Object object) {
        try {
            session.getRemote().sendString(BeerUtils.json(object));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
	}
}
