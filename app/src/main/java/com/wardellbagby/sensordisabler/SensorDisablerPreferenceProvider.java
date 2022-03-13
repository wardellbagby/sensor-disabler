package com.wardellbagby.sensordisabler;

import com.crossbowffs.remotepreferences.RemotePreferenceProvider;
import com.wardellbagby.sensordisabler.util.Constants;

public class SensorDisablerPreferenceProvider extends RemotePreferenceProvider {
  public SensorDisablerPreferenceProvider() {
    super("com.wardellbagby.sensordisabler", new String[] { Constants.PREFS_FILE_NAME });
  }
}
