package com.example.Backend_J2EE.dto.review;

public class ReviewMediaDto {
    private Integer id;
    private String mediaType;
    private String mediaUrl;

    public ReviewMediaDto() {
    }

    public ReviewMediaDto(Integer id, String mediaType, String mediaUrl) {
        this.id = id;
        this.mediaType = mediaType;
        this.mediaUrl = mediaUrl;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMediaType() {
        return mediaType;
    }

    public void setMediaType(String mediaType) {
        this.mediaType = mediaType;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }
}
