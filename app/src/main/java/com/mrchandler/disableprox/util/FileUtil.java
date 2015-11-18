package com.mrchandler.disableprox.util;

import android.os.Environment;

import java.io.File;
import java.io.IOException;

public final class FileUtil {
    private static final String PROX_FILES_DIRECTORY = "/disableprox/files/";
    private static final String PROX_FILE_NAME = "disable_prox_file.dat";
    private static final String PROX_SETTINGS_FILE_NAME = "/disableprox/files/disable_prox_settings.dat";

    private FileUtil() {
    }

    public static File getEnabledSettingsFile() {
        File file = new File(Environment.getExternalStorageDirectory() + PROX_FILE_NAME);
        if (!file.exists()) {
            new File(PROX_FILES_DIRECTORY).mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                return file;
            }

        }
        return file;
    }

    public static File getEnabledMethodsFile() {
        File file = new File(Environment.getExternalStorageDirectory() + PROX_SETTINGS_FILE_NAME);
        if (!file.exists()) {
            new File(PROX_FILES_DIRECTORY).mkdirs();
            try {
                file.createNewFile();
            } catch (IOException e) {
                return file;
            }

        }
        return file;
    }
}
