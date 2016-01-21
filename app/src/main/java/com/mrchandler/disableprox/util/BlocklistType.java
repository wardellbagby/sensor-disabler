package com.mrchandler.disableprox.util;

/**
 * @author Wardell
 */
public enum BlocklistType {
    BLACKLIST("blacklist"), WHITELIST("whitelist");

    String value;

    BlocklistType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
