package com.example.demo.dto;

/**
 * 图片上传响应
 */
public class ImageUploadResponse {

    /** 图片访问URL */
    private String url;

    /** 原始文件名 */
    private String originalName;

    /** 保存后的文件名 */
    private String savedName;

    /** 文件大小（字节） */
    private long size;

    public ImageUploadResponse() {}

    public ImageUploadResponse(String url, String originalName, String savedName, long size) {
        this.url = url;
        this.originalName = originalName;
        this.savedName = savedName;
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getSavedName() {
        return savedName;
    }

    public void setSavedName(String savedName) {
        this.savedName = savedName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
