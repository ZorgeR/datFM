/*
Copyright 2009 David Revell

This file is part of SwiFTP.

SwiFTP is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

SwiFTP is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.zlab.datFM.swiftp.server;

import android.util.Log;

public class CmdFEAT extends FtpCmd implements Runnable {
    private static final String TAG = CmdFEAT.class.getSimpleName();

    public static final String message = "TEMPLATE!!";

    public CmdFEAT(SessionThread sessionThread, String input) {
        super(sessionThread);
    }

    @Override
    public void run() {
        // sessionThread.writeString("211 No extended features\r\n");
        sessionThread.writeString("211-Features supported\r\n");
        sessionThread.writeString(" UTF8\r\n"); // advertise UTF8 support (fixes bug 14)
        sessionThread.writeString("211 End\r\n");
        Log.d(TAG, "Gave FEAT response");
    }

}
