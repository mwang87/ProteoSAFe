package edu.ucsd.livesearch.util;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

public class SessionListener implements HttpSessionListener
{
	// Session ID map
	private static Map<String, HttpSession> sessions =
		new HashMap<String, HttpSession>();
	
	public void sessionCreated(HttpSessionEvent event) {
		sessions.put(event.getSession().getId(), event.getSession());
	}
	
	public static HttpSession getSessionFromId(String id) {
		HttpSession session = sessions.get(id);
		// make sure the session isn't invalidated
		if (session != null) try {
			session.getAttribute("test");
		} catch (IllegalStateException error) {
			session = null;
			sessions.remove(id);
		}
		return session;
	}
	
	public void sessionDestroyed(HttpSessionEvent event) {
		sessions.remove(event.getSession().getId());
	}
}
