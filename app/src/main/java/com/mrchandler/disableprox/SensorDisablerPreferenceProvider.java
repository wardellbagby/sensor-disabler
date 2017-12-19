package com.mrchandler.disableprox;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;
import com.mrchandler.disableprox.util.Constants;

/**
 * @author Wardell Bagby
 */

public class SensorDisablerPreferenceProvider extends RemotePreferenceProvider {
    public SensorDisablerPreferenceProvider() {
        super("com.mrchandler.disableprox", new String[]{Constants.PREFS_FILE_NAME});
    }
}
