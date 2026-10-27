/*
 * Copyright (C) 2026
 *
 * Squore is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.doubleyellow.scoreboard.dialog;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.doubleyellow.scoreboard.Brand;
import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Call;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.model.Player;
import com.doubleyellow.scoreboard.prefs.ColorPrefs;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

/**
 * Video Ref (video review) flow. Started from the Appeal dialog by tapping the 'VR' button.
 * The referee always taps the name of the player that REQUESTS the video review.
 *
 * Steps:
 * 1) choose the decision that is under review: 'Stroke to opponent', 'Yes Let' or 'No Let to requester'
 * 2) announcement 'Video review requested by X on the decision Y'
 * 3) outcome: 'Overturned' or 'Decision stands'
 * 4) (only if overturned) choose the new decision
 * 5) announcement of the outcome. Only when this one is confirmed the decision is applied to the score.
 *
 * The original decision is NOT yet entered in the score when this dialog starts.
 */
public class VideoRef extends BaseAlertDialog
{
    public VideoRef(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    /** Video Ref is only available for squash and if it is enabled in the settings */
    public static boolean isActive(Context context) {
        return Brand.isSquash() && PreferenceValues.useVideoRef(context);
    }

    public enum Step {
        ChooseOriginalDecision,
        AnnounceRequest,
        ChooseOutcome,
        ChooseNewDecision,
        AnnounceResult,
    }

    /** The possible decisions. The 'appellant' of the decision (for the call) is determined separately */
    public enum Decision {
        Stroke(Call.ST),
        YesLet(Call.YL),
        NoLet (Call.NL),
        ;
        private final Call call;
        Decision(Call call) {
            this.call = call;
        }
        public Call getCall() {
            return call;
        }
    }

    private static final String KEY_REQUESTER = "VR_Requester";
    private static final String KEY_STEP      = "VR_Step";
    private static final String KEY_ORIGINAL  = "VR_Original";
    private static final String KEY_NEW       = "VR_New";

    private Player   m_requester = null;
    private Step     m_step      = Step.ChooseOriginalDecision;
    private Decision m_original  = null;
    /** null as long as no new decision is chosen (or decision stands) */
    private Decision m_new       = null;

    @Override public boolean storeState(Bundle outState) {
        if ( m_requester == null ) { return false; }
        outState.putSerializable(KEY_REQUESTER, m_requester);
        outState.putSerializable(KEY_STEP     , m_step);
        outState.putSerializable(KEY_ORIGINAL , m_original);
        outState.putSerializable(KEY_NEW      , m_new);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        m_requester = (Player)   outState.getSerializable(KEY_REQUESTER);
        m_step      = (Step)     outState.getSerializable(KEY_STEP);
        m_original  = (Decision) outState.getSerializable(KEY_ORIGINAL);
        m_new       = (Decision) outState.getSerializable(KEY_NEW);
        if ( m_step == null ) {
            m_step = Step.ChooseOriginalDecision;
        }
        if ( (m_original == null) && (m_step.equals(Step.ChooseOriginalDecision) == false) ) {
            // should not happen: restart the flow
            m_step = Step.ChooseOriginalDecision;
        }
        // note: m_new == null in step AnnounceResult means: decision stands
        return m_requester != null;
    }

    public void init(Player requester) {
        m_requester = requester;
        m_step      = Step.ChooseOriginalDecision;
        m_original  = null;
        m_new       = null;
    }

    private static final int BTN_DECISION_STROKE  = 2001;
    private static final int BTN_DECISION_YES_LET = 2002;
    private static final int BTN_DECISION_NO_LET  = 2003;
    private static final int BTN_OVERTURNED       = 2011;
    private static final int BTN_STANDS           = 2012;

    @Override public void show() {
        if ( (m_requester == null) || (matchModel == null) ) { return; }

        // a fresh builder for each step
        adb = new MyDialogBuilder(context);
        m_lButtons.clear();

        String sRequester = getName(m_requester);
        switch (m_step) {
            case ChooseOriginalDecision: {
                adb.setIcon (R.drawable.microphone);
                adb.setTitle(getOAString(R.string.oal_video_ref) + " - " + sRequester);
                adb.setMessage(getOAString(R.string.oal_vr_on_which_decision));
                LinearLayout ll = getVerticalLayout();
                // a player only complains about a stroke given to the opponent, never about his own
                addButton(ll, getDecisionLabel(Decision.Stroke, m_requester.getOther()), BTN_DECISION_STROKE );
                addButton(ll, getDecisionLabel(Decision.YesLet, m_requester           ), BTN_DECISION_YES_LET);
                addButton(ll, getDecisionLabel(Decision.NoLet , m_requester           ), BTN_DECISION_NO_LET );
                adb.setView(ll);
                ColorPrefs.setColor(ll);
                adb.setNegativeButton(R.string.cmd_cancel, null);
                dialog = adb.show();
                break;
            }
            case AnnounceRequest: {
                adb.setIcon (R.drawable.microphone);
                adb.setTitle(getOAString(R.string.oal_video_ref));
                adb.setMessage(getOAString(R.string.oa_vr_requested_by_x_on_decision_y, sRequester, getDecisionLabel(m_original, getAppellant())));
                adb.setPositiveButton(R.string.cmd_ok, onClickListener);
                adb.setNegativeButton(R.string.cmd_cancel, null);
                dialog = adb.show();
                break;
            }
            case ChooseOutcome: {
                adb.setIcon (R.drawable.microphone);
                adb.setTitle(getOAString(R.string.oal_video_ref));
                adb.setMessage(getDecisionLabel(m_original, getAppellant()));
                LinearLayout ll = getVerticalLayout();
                addButton(ll, getOAString(R.string.oa_vr_overturned), BTN_OVERTURNED);
                addButton(ll, getOAString(R.string.oa_vr_stands    ), BTN_STANDS    );
                adb.setView(ll);
                ColorPrefs.setColor(ll);
                adb.setNegativeButton(R.string.cmd_cancel, null);
                dialog = adb.show();
                break;
            }
            case ChooseNewDecision: {
                Player appellant = getAppellant();
                adb.setIcon (R.drawable.microphone);
                adb.setTitle(getOAString(R.string.oal_video_ref) + " - " + getOAString(R.string.oa_vr_overturned));
                adb.setMessage(getOAString(R.string.oal_vr_new_decision));
                LinearLayout ll = getVerticalLayout();
                // the new decision is for the same appellant as the original decision. Do not offer the original decision again
                if ( Decision.Stroke.equals(m_original) == false ) {
                    addButton(ll, getDecisionLabel(Decision.Stroke, appellant), BTN_DECISION_STROKE );
                }
                if ( Decision.YesLet.equals(m_original) == false ) {
                    addButton(ll, getDecisionLabel(Decision.YesLet, appellant), BTN_DECISION_YES_LET);
                }
                if ( Decision.NoLet.equals(m_original) == false ) {
                    addButton(ll, getDecisionLabel(Decision.NoLet , appellant), BTN_DECISION_NO_LET );
                }
                adb.setView(ll);
                ColorPrefs.setColor(ll);
                adb.setNegativeButton(R.string.cmd_cancel, null);
                dialog = adb.show();
                break;
            }
            case AnnounceResult: {
                String sMsg;
                if ( m_new == null ) {
                    // decision stands: requesting player loses one video ref
                    int iLeft = Math.max(0, matchModel.getNrOfVideoRefsLeftFor(m_requester) - 1);
                    sMsg = getOAString(R.string.oa_vr_decision_stands_x__y_has_n_left, getDecisionLabel(m_original, getAppellant()), sRequester, iLeft);
                } else {
                    // decision overturned: requesting player keeps his video ref
                    sMsg = getOAString(R.string.oa_vr_decision_overturned_x__y_retains, getDecisionLabel(m_new, getAppellant()), sRequester);
                }
                adb.setIcon (R.drawable.microphone);
                adb.setTitle(getOAString(R.string.oal_video_ref));
                adb.setMessage(sMsg);
                adb.setCancelable(false); // decision must be confirmed to be applied
                adb.setPositiveButton(R.string.cmd_ok, onClickListener);
                dialog = adb.show();
                break;
            }
        }
    }

    private final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialog, int which) {
            handleButtonClick(which);
        }
    };

    @Override public void handleButtonClick(int which) {
        if ( m_requester == null ) { return; }
        switch (m_step) {
            case ChooseOriginalDecision: {
                Decision d = getDecision(which);
                if ( d == null ) { return; }
                m_original = d;
                m_new      = null;
                m_step     = Step.AnnounceRequest;
                show();
                break;
            }
            case AnnounceRequest: {
                if ( which != DialogInterface.BUTTON_POSITIVE ) { return; }
                m_step = Step.ChooseOutcome;
                show();
                break;
            }
            case ChooseOutcome: {
                if ( which == BTN_STANDS ) {
                    m_new  = null;
                    m_step = Step.AnnounceResult;
                    show();
                } else if ( which == BTN_OVERTURNED ) {
                    m_step = Step.ChooseNewDecision;
                    show();
                }
                break;
            }
            case ChooseNewDecision: {
                Decision d = getDecision(which);
                if ( d == null ) { return; }
                m_new  = d;
                m_step = Step.AnnounceResult;
                show();
                break;
            }
            case AnnounceResult: {
                if ( which != DialogInterface.BUTTON_POSITIVE ) { return; }
                applyDecision();
                break;
            }
        }
    }

    private void applyDecision() {
        if ( (m_requester == null) || (m_original == null) ) { return; }
        Player   requester       = m_requester;
        Player   appellant       = getAppellant();
        boolean  bDecisionStands = (m_new == null);
        Decision finalDecision   = bDecisionStands ? m_original : m_new;

        // ensure it can not be applied twice
        m_requester = null;

        scoreBoard.recordVideoRef(requester, bDecisionStands, appellant, finalDecision.getCall());
    }

    /**
     * The 'appellant' of the original let appeal:
     * - for 'Stroke to opponent' it is the opponent of the requesting player
     * - for 'Yes Let' and 'No Let' it is the requesting player
     */
    private Player getAppellant() {
        if ( Decision.Stroke.equals(m_original) ) {
            return m_requester.getOther();
        }
        return m_requester;
    }

    private static Decision getDecision(int which) {
        switch (which) {
            case BTN_DECISION_STROKE : return Decision.Stroke;
            case BTN_DECISION_YES_LET: return Decision.YesLet;
            case BTN_DECISION_NO_LET : return Decision.NoLet;
        }
        return null;
    }

    private String getDecisionLabel(Decision decision, Player appellant) {
        if ( decision == null ) { return ""; }
        switch (decision) {
            case Stroke: return getOAString(R.string.oa_stroke_to_x   , getName(appellant));
            case YesLet: return getOAString(R.string.oa_vr_let);
            case NoLet : return getOAString(R.string.oa_vr_no_let_to_x, getName(appellant));
        }
        return "";
    }

    private String getName(Player p) {
        if ( p == null ) { return ""; }
        return matchModel.getName_no_nbsp(p, false);
    }

    private LinearLayout getVerticalLayout() {
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);
        return ll;
    }

    private void addButton(LinearLayout ll, String sCaption, int iAction) {
        TextView tv = getActionView(sCaption, iAction, 0, 0, null);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        tv.setPadding(20, 30, 20, 30);
        LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        llp.setMargins(10, 10, 10, 10);
        ll.addView(tv, llp);
    }

    private String getOAString(int iResId, Object... formats) {
        return PreferenceValues.getOAString(context, iResId, formats);
    }
}
