package com.forest.ape.nio;

import java.io.PrintWriter;

import com.forest.ape.exception.ApeException;

public interface SessionTracker {
    public static interface Session {
        long getSessionId();
        int getTimeout();
        boolean isClosing();
    }
    public static interface SessionExpirer {
        void expire(Session session);

        long getServerId();
    }

    long createSession(int sessionTimeout);

    void addSession(long id, int to);

    /**
     * @param sessionId
     * @param sessionTimeout
     * @return false if session is no longer active
     */
    boolean touchSession(long sessionId, int sessionTimeout);

    /**
     * Mark that the session is in the process of closing.
     * @param sessionId
     */
    void setSessionClosing(long sessionId);

    /**
     * 
     */
    void shutdown();

    /**
     * @param sessionId
     */
    void removeSession(long sessionId);

    void checkSession(long sessionId, Object owner) throws ApeException;

    void setOwner(long id, Object owner) throws ApeException;

    /**
     * Text dump of session information, suitable for debugging.
     * @param pwriter the output writer
     */
    void dumpSessions(PrintWriter pwriter);
}
