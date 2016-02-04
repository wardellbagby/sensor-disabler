package com.mrchandler.disableprox.util;

/**
 * @author Wardell
 */
public enum BlocklistType {
    BLACKLIST("blacklist"), WHITELIST("whitelist"), NONE("none");

    String value;

    BlocklistType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
