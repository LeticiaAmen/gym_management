package com.gym.gym_management.controller.dto;

import java.time.LocalDateTime;

public class ActivityDto {
    private final String type;
    private final String title;
    private final String description;
    private final LocalDateTime timestamp;
    private final Long relatedId;

    public ActivityDto(String type, String title, String description, LocalDateTime timestamp, Long relatedId) {
        this.type = type;
        this.title = title;
        this.description = description;
        this.timestamp = timestamp;
        this.relatedId = relatedId;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public Long getRelatedId() {
        return relatedId;
    }
}
