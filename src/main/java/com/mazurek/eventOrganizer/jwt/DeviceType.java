package com.mazurek.eventOrganizer.jwt;

public enum DeviceType {

    MOBILE_ANDROID(false),
    MOBILE_IOS(false),
    MOBILE_OTHER(false),
    TABLET_ANDROID(false),
    TABLET_IOS(false),
    TABLET_OTHER(false),
    DESKTOP(true),
    WEB(true),
    UNKNOWN(true);

    private final boolean rotateRefreshToken;

    DeviceType(boolean rotateRefreshToken) {
        this.rotateRefreshToken = rotateRefreshToken;
    }

    public boolean shouldRotateRefreshToken() {
        return rotateRefreshToken;
    }
}