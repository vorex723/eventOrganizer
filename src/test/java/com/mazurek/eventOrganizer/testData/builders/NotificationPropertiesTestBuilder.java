package com.mazurek.eventOrganizer.testData.builders;

import com.mazurek.eventOrganizer.config.properties.NotificationProperties;
import com.mazurek.eventOrganizer.config.properties.NotificationProperties.Delivery;
import com.mazurek.eventOrganizer.config.properties.NotificationProperties.Devices;
import com.mazurek.eventOrganizer.config.properties.NotificationProperties.Email;
import com.mazurek.eventOrganizer.config.properties.NotificationProperties.Retention;



/** Constructs data only; explicit overrides are passed through without repair. */
public class NotificationPropertiesTestBuilder {
    private Delivery delivery;
    private boolean deliverySet;
    private Devices devices;
    private boolean devicesSet;
    private Email email;
    private boolean emailSet;
    private Retention retention;
    private boolean retentionSet;

    public NotificationPropertiesTestBuilder delivery(Delivery delivery) {
        this.delivery = NotificationDeliveryPropertiesTestBuilder.copyOf(delivery);
        this.deliverySet = true;
        return this;
    }

    public NotificationPropertiesTestBuilder devices(Devices devices) {
        this.devices = NotificationDevicesPropertiesTestBuilder.copyOf(devices);
        this.devicesSet = true;
        return this;
    }

    public NotificationPropertiesTestBuilder email(Email email) {
        this.email = NotificationEmailPropertiesTestBuilder.copyOf(email);
        this.emailSet = true;
        return this;
    }

    public NotificationPropertiesTestBuilder retention(Retention retention) {
        this.retention = NotificationRetentionPropertiesTestBuilder.copyOf(retention);
        this.retentionSet = true;
        return this;
    }

    public static NotificationProperties copyOf(NotificationProperties source) {
        if (source == null) return null;
        return new NotificationPropertiesTestBuilder()
                .delivery(source.getDelivery())
                .devices(source.getDevices())
                .email(source.getEmail())
                .retention(source.getRetention())
                .build();
    }


    public NotificationProperties build() {
        NotificationProperties value = new NotificationProperties();
        value.setDelivery(deliverySet ? NotificationDeliveryPropertiesTestBuilder.copyOf(delivery) : new NotificationDeliveryPropertiesTestBuilder().build());
        value.setDevices(devicesSet ? NotificationDevicesPropertiesTestBuilder.copyOf(devices) : new NotificationDevicesPropertiesTestBuilder().build());
        value.setEmail(emailSet ? NotificationEmailPropertiesTestBuilder.copyOf(email) : new NotificationEmailPropertiesTestBuilder().build());
        value.setRetention(retentionSet ? NotificationRetentionPropertiesTestBuilder.copyOf(retention) : new NotificationRetentionPropertiesTestBuilder().build());
        return value;
    }
}
