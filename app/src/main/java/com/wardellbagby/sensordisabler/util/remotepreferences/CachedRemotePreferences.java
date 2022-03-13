package com.wardellbagby.sensordisabler.util.remotepreferences;

import android.content.Context;
import android.content.SharedPreferences;
import com.crossbowffs.remotepreferences.RemotePreferences;
import java.util.Map;
import java.util.Set;

/**
 * RemotePreferences that cache the key-value pairs in memory, updating when the backing
 * SharedPreferences
 * are updated.
 */
public class CachedRemotePreferences extends RemotePreferences {

  private Map<String, ?> cache;

  public CachedRemotePreferences(Context context, String authority, String prefName) {
    this(context, authority, prefName, false);
  }

  public CachedRemotePreferences(Context context, String authority, String prefName,
      boolean strictMode) {
    super(context, authority, prefName, strictMode);
    cache = getAll();
    registerOnSharedPreferenceChangeListener(new OnSharedPreferenceChangeListener() {
      @Override
      public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        cache = sharedPreferences.getAll();
      }
    });
  }

  @Override
  public String getString(String key, String defValue) {
    Object value = cache.get(key);
    if (value == null || !(value instanceof String)) {
      return defValue;
    }
    return (String) value;
  }

  @Override
  public Set<String> getStringSet(String key, Set<String> defValues) {
    Object value = cache.get(key);
    if (value == null || !(value instanceof Set)) {
      return defValues;
    }
    //noinspection unchecked All sets will beSet<String>. If not, Android has added a new type to SharedPreferences that should be supported.
    return (Set<String>) value;
  }

  @Override
  public int getInt(String key, int defValue) {
    Object value = cache.get(key);
    if (value == null || !(value instanceof Integer)) {
      return defValue;
    }
    return (Integer) value;
  }

  @Override
  public long getLong(String key, long defValue) {
    Object value = cache.get(key);
    if (value == null || !(value instanceof Long)) {
      return defValue;
    }
    return (Long) value;
  }

  @Override
  public float getFloat(String key, float defValue) {
    Object value = cache.get(key);
    if (value == null || !(value instanceof Float)) {
      return defValue;
    }
    return (Float) value;
  }

  @Override
  public boolean getBoolean(String key, boolean defValue) {
    Object value = cache.get(key);
    if (value == null || !(value instanceof Boolean)) {
      return defValue;
    }
    return (Boolean) value;
  }

  @Override
  public boolean contains(String key) {
    return cache.containsKey(key);
  }
}