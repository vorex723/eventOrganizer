package com.mazurek.eventOrganizer.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationProperties {

    @NotNull
    @Min(1)
    private Integer defaultPageSize;

}
