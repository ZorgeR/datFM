/*
Copyright 2011-2013 Pieter Pareit
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

package com.zlab.datFM.swiftp;

import java.io.File;

import android.content.Context;

// TODO: this must all be removed
//       if you need a setting, get it from the settings

public class Globals {
    private static String lastError;
    private static File chrootDir = null;
    private static String username = null;

    public static File getChrootDir() {
        return chrootDir;
    }

    public static void setChrootDir(File chrootDir) {
        if (chrootDir.isDirectory()) {
            Globals.chrootDir = chrootDir;
        }
    }

    public static String getLastError() {
        return lastError;
    }

    public static void setLastError(String lastError) {
        Globals.lastError = lastError;
    }

    public static String getUsername() {
        return username;
    }

    public static void setUsername(String username) {
        Globals.username = username;
    }

}
