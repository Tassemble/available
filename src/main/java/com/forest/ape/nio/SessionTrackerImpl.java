package com.forest.ape.nio;


import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.forest.ape.exception.ApeException;

/**
 * This is a full featured SessionTracker. It tracks session in grouped by tick
 * interval. It always rounds up the tick interval to provide a sort of grace
 * period. Sessions are thus expired in batches made up of sessions that expire
 * in a given interval.
 */
public class SessionTrackerImpl extends Thread implements SessionTracker {
    private static final Logger LOG = LoggerFactory.getLogger(SessionTrackerImpl.class);

    HashMap<Long, SessionImpl> sessionsById = new HashMap<Long, SessionImpl>();

    HashMap<Long, SessionSet> sessionSets = new HashMap<Long, SessionSet>();

    ConcurrentHashMap<Long, Integer> sessionsWithTimeout;
    long nextSessionId = 0;
    long nextExpirationTime;

    int expirationInterval;

    public static class SessionImpl implements Session {
        SessionImpl(long sessionId, int timeout, long expireTime) {
            this.sessionId = sessionId;
            this.timeout = timeout;
            this.tickTime = expireTime;
            isClosing = false;
        }

        final long sessionId;
        final int timeout;
        long tickTime;
        boolean isClosing;

        Object owner;

        public long getSessionId() { return sessionId; }
        public int getTimeout() { return timeout; }
        public boolean isClosing() { return isClosing; }
    }

    public static long initializeNextSession(long id) {
        long nextSid = 0;
        nextSid = (System.currentTimeMillis() << 24) >> 8;
        nextSid =  nextSid | (id <<56);
        return nextSid;
    }

    static class SessionSet {
        HashSet<SessionImpl> sessions = new HashSet<SessionImpl>();
    }

    SessionExpirer expirer;

    private long roundToInterval(long time) {
        // We give a one interval grace period
        return (time / expirationInterval + 1) * expirationInterval;
    }

    public SessionTrackerImpl(SessionExpirer expirer,
            ConcurrentHashMap<Long, Integer> sessionsWithTimeout, int tickTime,
            long sid)
    {
        super("SessionTracker");
        this.expirer = expirer;
        this.expirationInterval = tickTime;
        this.sessionsWithTimeout = sessionsWithTimeout;
        nextExpirationTime = roundToInterval(System.currentTimeMillis());
        this.nextSessionId = initializeNextSession(sid);
        for (Entry<Long, Integer> e : sessionsWithTimeout.entrySet()) {
            addSession(e.getKey(), e.getValue());
        }
    }

    volatile boolean running = true;

    volatile long currentTime;

    synchronized public void dumpSessions(PrintWriter pwriter) {
        pwriter.print("Session Sets (");
        pwriter.print(sessionSets.size());
        pwriter.println("):");
        ArrayList<Long> keys = new ArrayList<Long>(sessionSets.keySet());
        Collections.sort(keys);
        for (long time : keys) {
            pwriter.print(sessionSets.get(time).sessions.size());
            pwriter.print(" expire at ");
            pwriter.print(new Date(time));
            pwriter.println(":");
            for (SessionImpl s : sessionSets.get(time).sessions) {
                pwriter.print("\t0x");
                pwriter.println(Long.toHexString(s.sessionId));
            }
        }
    }

    @Override
    synchronized public String toString() {
        StringWriter sw = new StringWriter();
        PrintWriter pwriter = new PrintWriter(sw);
        dumpSessions(pwriter);
        pwriter.flush();
        pwriter.close();
        return sw.toString();
    }

    @Override
    synchronized public void run() {
        try {
            while (running) {
                currentTime = System.currentTimeMillis();
                if (nextExpirationTime > currentTime) {
                    this.wait(nextExpirationTime - currentTime);
                    continue;
                }
                SessionSet set;
                set = sessionSets.remove(nextExpirationTime);
                if (set != null) {
                    for (SessionImpl s : set.sessions) {
                        sessionsById.remove(s.sessionId);
                        expirer.expire(s);
                    }
                }
                nextExpirationTime += expirationInterval;
            }
        } catch (InterruptedException e) {
            LOG.error("Unexpected interruption", e);
        }
        LOG.info("SessionTrackerImpl exited loop!");
    }

    synchronized public boolean touchSession(long sessionId, int timeout) {
        if (LOG.isTraceEnabled()) {
            LOG.trace("SessionTrackerImpl --- Touch session: 0x"
                    + Long.toHexString(sessionId) + " with timeout " + timeout);
        }
        SessionImpl s = sessionsById.get(sessionId);
        if (s == null) {
            return false;
        }
        long expireTime = roundToInterval(System.currentTimeMillis() + timeout);
        if (s.tickTime >= expireTime) {
            // Nothing needs to be done
            return true;
        }
        SessionSet set = sessionSets.get(s.tickTime);
        if (set != null) {
            set.sessions.remove(s);
        }
        s.tickTime = expireTime;
        set = sessionSets.get(s.tickTime);
        if (set == null) {
            set = new SessionSet();
            sessionSets.put(expireTime, set);
        }
        set.sessions.add(s);
        return true;
    }

    synchronized public void setSessionClosing(long sessionId) {
        if (LOG.isTraceEnabled()) {
            LOG.info("Session closing: 0x" + Long.toHexString(sessionId));
        }
        SessionImpl s = sessionsById.get(sessionId);
        if (s == null) {
            return;
        }
        s.isClosing = true;
    }

    synchronized public void removeSession(long sessionId) {
        SessionImpl s = sessionsById.remove(sessionId);
        sessionsWithTimeout.remove(sessionId);
        if (LOG.isTraceEnabled()) {
            LOG.trace("SessionTrackerImpl --- Removing session 0x"
                    + Long.toHexString(sessionId));
        }
        if (s != null) {
            sessionSets.get(s.tickTime).sessions.remove(s);
        }
    }

    public void shutdown() {
        LOG.info("Shutting down");

        running = false;
        if (LOG.isTraceEnabled()) {
            LOG.trace("Shutdown SessionTrackerImpl!");
        }
    }


    synchronized public long createSession(int sessionTimeout) {
        addSession(nextSessionId, sessionTimeout);
        return nextSessionId++;
    }

    synchronized public void addSession(long id, int sessionTimeout) {
        sessionsWithTimeout.put(id, sessionTimeout);
        if (sessionsById.get(id) == null) {
            SessionImpl s = new SessionImpl(id, sessionTimeout, 0);
            sessionsById.put(id, s);
            if (LOG.isTraceEnabled()) {
            	LOG.trace("SessionTrackerImpl --- Adding session 0x"
                        + Long.toHexString(id) + " " + sessionTimeout);
            }
        } else {
            if (LOG.isTraceEnabled()) {
                LOG.trace("SessionTrackerImpl --- Existing session 0x"
                        + Long.toHexString(id) + " " + sessionTimeout);
            }
        }
        touchSession(id, sessionTimeout);
    }

    synchronized public void checkSession(long sessionId, Object owner) throws ApeException {
        SessionImpl session = sessionsById.get(sessionId);
        if (session == null || session.isClosing()) {
            throw new ApeException.SessionExpiredException();
        }
        if (session.owner == null) {
            session.owner = owner;
        } else if (session.owner != owner) {
        	throw new ApeException.SessionMovedException();
        }
    }

    synchronized public void setOwner(long id, Object owner) throws ApeException {
        SessionImpl session = sessionsById.get(id);
        if (session == null) {
        	throw new ApeException.SessionExpiredException();
        }
        session.owner = owner;
    }
}