package com.feedblocker;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static final String FILE = "feedblocker";
    static final String SCROLL_LIMIT = "scroll_limit";       // scrolls allowed per window
    static final String SESSION_MIN = "session_minutes";     // max minutes per visit (0 = off)
    static final String COOLDOWN_MIN = "cooldown_minutes";   // locked out after a kick
    static final String BLOCK_FB = "block_fb";
    static final String BLOCK_LITE = "block_lite";
    static final String LITE_FULL = "lite_full_block";
    static final String LOCKED_UNTIL = "locked_until";

    static SharedPreferences get(Context c) {
        return c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
    private Prefs() {}
}
