package com.feedblocker;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.ArrayDeque;
import java.util.regex.Pattern;
import android.widget.Toast;

/**
 * Facebook: counts real scroll events in the app and kicks you out after the limit.
 * FB Lite draws its own UI and often reports no scroll events, so a per-visit time
 * limit and a burst-of-screen-changes check back it up.
 */
public class FeedBlockerService extends AccessibilityService {
    private static final String FB = "com.facebook.katana";
    private static final String LITE = "com.facebook.lite";
    private static final long SCROLL_WINDOW_MS = 60_000;   // limit counts scrolls in the last minute
    private static final long SCROLL_DEBOUNCE_MS = 350;    // one fling sends many events: count it once

    private final long[] scrollTimes = new long[64];
    private int scrollCount = 0;
    private long lastScroll = 0;
    private long sessionStart = 0;
    private String currentPkg = null;
    private long lastKick = 0;
    private long lastEvent = 0;
    // FB Lite's built-in Messenger: chats are always allowed
    private static final Pattern CHAT_LABEL = Pattern.compile(
            "(?i)^(chats?|messages?|messenger|aa|type a message.*|write a message.*|message\\.*|search messenger.*|new message|active now)$");
    private long lastChatCheck = 0;
    private boolean inChats = false;
    // FB Lite fallback: frequent content changes while it is in front look like scrolling
    private long liteBurstStart = 0;
    private int liteBurst = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e.getPackageName() == null) return;
        String pkg = e.getPackageName().toString();
        SharedPreferences p = Prefs.get(this);
        if (pkg.equals(FB) && !p.getBoolean(Prefs.BLOCK_FB, true)) return;
        if (pkg.equals(LITE) && !p.getBoolean(Prefs.BLOCK_LITE, true)) return;
        long now = SystemClock.elapsedRealtime();
        if (now - lastKick < 1500) return;

        // In FB Lite's chats: no scroll limit, no time limit, even during a cooldown.
        if (pkg.equals(LITE) && isInChats(now)) {
            sessionStart = now;
            scrollCount = 0;
            liteBurst = 0;
            return;
        }

        // Still in cooldown from a previous kick: no access at all.
        if (System.currentTimeMillis() < p.getLong(Prefs.LOCKED_UNTIL, 0)) {
            kick("Feed Blocker: still on cooldown.", false);
            return;
        }

        // New visit: different app, or no events for 30 s (you left and came back).
        boolean newVisit = !pkg.equals(currentPkg) || sessionStart == 0 || now - lastEvent > 30_000;
        lastEvent = now;
        if (newVisit) {
            currentPkg = pkg;
            sessionStart = now;
            scrollCount = 0;
            liteBurst = 0;
        }

        int sessionMin = p.getInt(Prefs.SESSION_MIN, 3);
        if (sessionMin > 0 && now - sessionStart > sessionMin * 60_000L) {
            kick("Time's up on Facebook. Go do something real.", true);
            return;
        }

        int limit = p.getInt(Prefs.SCROLL_LIMIT, 0);
        if (e.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            if (now - lastScroll < SCROLL_DEBOUNCE_MS) return;
            lastScroll = now;
            if (countScroll(now) > limit) kick("No feed scrolling 🙅", true);
        } else if (pkg.equals(LITE) && e.getEventType() == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if (now - liteBurstStart > 2000) { liteBurstStart = now; liteBurst = 0; }
            if (++liteBurst >= 25) {           // sustained redraw = scrolling the feed
                liteBurst = 0;
                if (now - lastScroll > 1500) { lastScroll = now; if (countScroll(now) > limit) kick("No feed scrolling 🙅", true); }
            }
        }
    }

    /** True when FB Lite shows a chat or the chat list: a message box or a Chats/Messenger label. */
    private boolean isInChats(long now) {
        if (now - lastChatCheck < 400) return inChats;
        lastChatCheck = now;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        inChats = false;
        if (root == null) return false;
        ArrayDeque<AccessibilityNodeInfo> q = new ArrayDeque<>();
        q.add(root);
        int seen = 0;
        while (!q.isEmpty() && seen++ < 500) {
            AccessibilityNodeInfo n = q.poll();
            if (n == null) continue;
            if (n.isEditable() && n.isVisibleToUser()) { inChats = true; break; }
            CharSequence t = n.getText() != null ? n.getText() : n.getContentDescription();
            if (t != null && n.isVisibleToUser() && CHAT_LABEL.matcher(t.toString().trim()).matches()
                    && (n.isSelected() || n.isEditable()
                        || (android.os.Build.VERSION.SDK_INT >= 28 && n.isHeading()))) { inChats = true; break; }
            for (int i = 0; i < n.getChildCount(); i++) q.add(n.getChild(i));
        }
        return inChats;
    }

    private int countScroll(long now) {
        int n = 0;
        for (int i = 0; i < scrollCount; i++) if (now - scrollTimes[i] <= SCROLL_WINDOW_MS) scrollTimes[n++] = scrollTimes[i];
        if (n < scrollTimes.length) scrollTimes[n++] = now;
        scrollCount = n;
        return n;
    }

    private void kick(String msg, boolean startCooldown) {
        lastKick = SystemClock.elapsedRealtime();
        scrollCount = 0;
        sessionStart = 0;
        currentPkg = null;
        if (startCooldown) {
            int cd = Prefs.get(this).getInt(Prefs.COOLDOWN_MIN, 10);
            if (cd > 0) Prefs.get(this).edit()
                    .putLong(Prefs.LOCKED_UNTIL, System.currentTimeMillis() + cd * 60_000L).apply();
        }
        performGlobalAction(GLOBAL_ACTION_HOME);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override public void onInterrupt() {}
}
