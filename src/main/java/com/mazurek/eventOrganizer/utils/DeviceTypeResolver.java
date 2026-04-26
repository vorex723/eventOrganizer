package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.jwt.DeviceType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DeviceTypeResolver {

    private static final Set<String> VALID_DEVICE_TYPES = Arrays.stream(DeviceType.values())
            .map(Enum::name)
            .collect(Collectors.toSet());

    public DeviceType determineDeviceType(String deviceTypeHeader, String userAgent) {

        if (deviceTypeHeader != null && !deviceTypeHeader.isBlank()) {
            String normalizedHeader = deviceTypeHeader.trim().toUpperCase();

            if (VALID_DEVICE_TYPES.contains(normalizedHeader)) {
                try {
                    return DeviceType.valueOf(normalizedHeader);
                } catch (IllegalArgumentException e) {
                    log.warn("Failed to parse valid device type header: '{}'. This should not happen.",
                            deviceTypeHeader, e);
                }
            } else {
                log.warn("Invalid device type header received: '{}'. Valid values: {}. Falling back to User-Agent detection.",
                        deviceTypeHeader, VALID_DEVICE_TYPES);
            }
        }

        if (userAgent != null && !userAgent.isBlank()) {
            DeviceType detected = detectFromUserAgent(userAgent);
            log.debug("Detected device type from User-Agent: {}", detected);
            return detected;
        }

        log.debug("No device type header or User-Agent provided. Defaulting to WEB.");
        return DeviceType.WEB;
    }

    private DeviceType detectFromUserAgent(String userAgent) {
        String ua = userAgent.toLowerCase();


        if (isMobileDevice(ua)) {
            if (ua.contains("android")) {
                return isTablet(ua) ? DeviceType.TABLET_ANDROID : DeviceType.MOBILE_ANDROID;
            }
            if (ua.contains("iphone") || ua.contains("ipod")) {
                return DeviceType.MOBILE_IOS;
            }
            if (ua.contains("ipad")) {
                return DeviceType.TABLET_IOS;
            }
        }


        if (ua.contains("windows") || ua.contains("macintosh") || ua.contains("linux")) {
            return DeviceType.DESKTOP;
        }

        return DeviceType.WEB;
    }

    private boolean isMobileDevice(String userAgent) {
        return userAgent.contains("mobile")
                || userAgent.contains("android")
                || userAgent.contains("iphone")
                || userAgent.contains("ipad")
                || userAgent.contains("ipod");
    }

    private boolean isTablet(String userAgent) {
        return !userAgent.contains("mobile") && userAgent.contains("android");
    }
}
