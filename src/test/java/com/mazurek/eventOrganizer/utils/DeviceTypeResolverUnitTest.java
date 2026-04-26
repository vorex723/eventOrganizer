package com.mazurek.eventOrganizer.utils;

import com.mazurek.eventOrganizer.jwt.DeviceType;
import com.mazurek.eventOrganizer.testData.TestConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DeviceTypeResolver unit tests:")
class DeviceTypeResolverUnitTest {

    private final DeviceTypeResolver deviceTypeResolver = new DeviceTypeResolver();

    @Test
    @DisplayName("When valid device type header is provided should return header value")
    void whenValidDeviceTypeHeaderIsProvidedShouldReturnHeaderValue() {
        DeviceType resolved = deviceTypeResolver.determineDeviceType(DeviceType.WEB.name(), TestConstants.DeviceConstants.USER_AGENT_ANDROID_MOBILE);
        assertThat(resolved).isEqualTo(DeviceType.WEB);
    }

    @Test
    @DisplayName("When valid header has mixed case should normalize and return it")
    void whenValidHeaderHasMixedCaseShouldNormalizeAndReturnIt() {
        DeviceType resolved = deviceTypeResolver.determineDeviceType(
                DeviceType.MOBILE_ANDROID.name().toLowerCase(),
                TestConstants.DeviceConstants.USER_AGENT_DESKTOP_WINDOWS);

        assertThat(resolved).isEqualTo(DeviceType.MOBILE_ANDROID);
    }

    @Test
    @DisplayName("When header is invalid should fallback to user-agent detection")
    void whenHeaderIsInvalidShouldFallbackToUserAgentDetection() {
        DeviceType resolved = deviceTypeResolver.determineDeviceType(
                TestConstants.DeviceConstants.INVALID_DEVICE_TYPE_HEADER,
                TestConstants.DeviceConstants.USER_AGENT_IPHONE);

        assertThat(resolved).isEqualTo(DeviceType.MOBILE_IOS);
    }

    @Test
    @DisplayName("When user-agent represents android mobile should return MOBILE_ANDROID")
    void whenUserAgentRepresentsAndroidMobileShouldReturnMobileAndroid() {
        DeviceType resolved = deviceTypeResolver.determineDeviceType(
                null,
                TestConstants.DeviceConstants.USER_AGENT_ANDROID_MOBILE);

        assertThat(resolved).isEqualTo(DeviceType.MOBILE_ANDROID);
    }

    @Test
    @DisplayName("When user-agent represents android tablet should return TABLET_ANDROID")
    void whenUserAgentRepresentsAndroidTabletShouldReturnTabletAndroid() {
        DeviceType resolved = deviceTypeResolver.determineDeviceType(
                null,
                TestConstants.DeviceConstants.USER_AGENT_ANDROID_TABLET);

        assertThat(resolved).isEqualTo(DeviceType.TABLET_ANDROID);
    }

    @Test
    @DisplayName("When user-agent represents iPad should return TABLET_IOS")
    void whenUserAgentRepresentsIpadShouldReturnTabletIos() {
        DeviceType resolved = deviceTypeResolver.determineDeviceType(
                null,
                TestConstants.DeviceConstants.USER_AGENT_IPAD);

        assertThat(resolved).isEqualTo(DeviceType.TABLET_IOS);
    }

    @Test
    @DisplayName("When user-agent represents desktop should return DESKTOP")
    void whenUserAgentRepresentsDesktopShouldReturnDesktop() {
        DeviceType resolved = deviceTypeResolver.determineDeviceType(
                null,
                TestConstants.DeviceConstants.USER_AGENT_DESKTOP_WINDOWS);

        assertThat(resolved).isEqualTo(DeviceType.DESKTOP);
    }

    @Test
    @DisplayName("When no header and no user-agent should return WEB")
    void whenNoHeaderAndNoUserAgentShouldReturnWeb() {
        DeviceType resolved = deviceTypeResolver.determineDeviceType(null, null);
        assertThat(resolved).isEqualTo(DeviceType.WEB);
    }

    @Test
    @DisplayName("When user-agent is unknown should return WEB")
    void whenUserAgentIsUnknownShouldReturnWeb() {
        DeviceType resolved = deviceTypeResolver.determineDeviceType(null, TestConstants.DeviceConstants.USER_AGENT_UNKNOWN);
        assertThat(resolved).isEqualTo(DeviceType.WEB);
    }
}
