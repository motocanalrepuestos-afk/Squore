package com.doubleyellow.scoreboard.model;

import com.doubleyellow.scoreboard.R;

public enum Misc {
    /** Timeout */
    TO(R.string.timeout_string_abbreviation),
    /** Video Ref (video review) requested and the decision was overturned: player keeps his Video Ref */
    VRK(R.string.video_ref_kept_abbreviation),
    /** Video Ref (video review) requested and the decision stands: player loses one Video Ref */
    VRL(R.string.video_ref_lost_abbreviation),
    ;

    private int iResIdAbbr;
    Misc(int iResIdAbbr) {
        this.iResIdAbbr = iResIdAbbr;
    }
    public int getResourceIdAbbreviation() {
        return this.iResIdAbbr;
    }
}
