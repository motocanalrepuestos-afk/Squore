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

import com.doubleyellow.scoreboard.R;
import com.doubleyellow.scoreboard.main.ScoreBoard;
import com.doubleyellow.scoreboard.model.Model;
import com.doubleyellow.scoreboard.prefs.PreferenceValues;

/**
 * Announcement that the Video Refs of both players have been reset to 1.
 * - at 10-10 (tiebreak) in any game
 * - at the start of game 5
 */
public class VideoRefAnnouncement extends BaseAlertDialog
{
    public enum Reason {
        TieBreak,
        StartOfGame5,
    }

    public VideoRefAnnouncement(Context context, Model matchModel, ScoreBoard scoreBoard) {
        super(context, matchModel, scoreBoard);
    }

    private Reason m_reason = null;

    public void init(Reason reason) {
        m_reason = reason;
    }

    @Override public boolean storeState(Bundle outState) {
        outState.putSerializable(Reason.class.getSimpleName(), m_reason);
        return true;
    }

    @Override public boolean init(Bundle outState) {
        init((Reason) outState.getSerializable(Reason.class.getSimpleName()));
        return true;
    }

    @Override public void show() {
        if ( m_reason == null ) { return; }
        String sMsg;
        boolean bTwoClearPoints = (matchModel == null) || matchModel.getTiebreakFormat().needsTwoClearPoints();
        if ( Reason.TieBreak.equals(m_reason) && bTwoClearPoints ) {
            sMsg = PreferenceValues.getOAString(context, R.string.oa_vr_two_clear_points__each_has_1);
        } else {
            sMsg = PreferenceValues.getOAString(context, R.string.oa_vr_each_has_1);
        }
        adb.setIcon   (R.drawable.microphone);
        adb.setTitle  (PreferenceValues.getOAString(context, R.string.oal_video_ref));
        adb.setMessage(sMsg);
        adb.setPositiveButton(R.string.cmd_ok, onClickListener);
        adb.setOnKeyListener(getOnBackKeyListener(DialogInterface.BUTTON_POSITIVE));
        adb.setCancelable(false); // ensure the dialog stack always continues via handleButtonClick()
        try {
            dialog = adb.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final DialogInterface.OnClickListener onClickListener = new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface dialogInterface, int which) {
            handleButtonClick(which);
        }
    };

    @Override public void handleButtonClick(int which) {
        // continue with possible other dialogs on the stack
        showNextDialog();
    }
}
