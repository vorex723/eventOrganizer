package com.mazurek.eventOrganizer.notification.delivery;

public record NotificationSendResult(
        boolean success,
        String providerMessageId,
        String errorMessage
) {
    public static NotificationSendResult sent( String providerMessageId){
        return new NotificationSendResult(true, providerMessageId, null);
    }
    public static NotificationSendResult failed(String errorMessage){
        return new NotificationSendResult(false, null, errorMessage);
    }
}
