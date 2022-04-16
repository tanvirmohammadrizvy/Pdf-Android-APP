package com.fulcrumy.pdfeditor.models;

public class PdfModel {
    private String absolutePath;
    private String createdAt;
    private boolean isDirectory;
    private boolean isStarred;
    private Long lastModified;
    private Long length;
    private String name;
    private int numItems;
    private String pdfUri;
    private String thumbUri;

    public PdfModel() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String str) {
        this.name = str;
    }

    public Long getLength() {
        return this.length;
    }

    public void setLength(Long l) {
        this.length = l;
    }

    public String getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(String str) {
        this.createdAt = str;
    }

    public String getAbsolutePath() {
        return this.absolutePath;
    }

    public void setAbsolutePath(String str) {
        this.absolutePath = str;
    }

    public Long getLastModified() {
        return this.lastModified;
    }

    public void setLastModified(Long l) {
        this.lastModified = l;
    }

    public String getPdfUri() {
        return this.pdfUri;
    }

    public void setPdfUri(String uri) {
        this.pdfUri = uri;
    }

    public String getThumbUri() {
        return this.thumbUri;
    }

    public void setThumbUri(String uri) {
        this.thumbUri = uri;
    }

    public boolean isStarred() {
        return this.isStarred;
    }

    public void setStarred(boolean z) {
        this.isStarred = z;
    }

    public boolean isDirectory() {
        return this.isDirectory;
    }

    public void setDirectory(boolean z) {
        this.isDirectory = z;
    }

    public int getNumItems() {
        return this.numItems;
    }

    public void setNumItems(int i) {
        this.numItems = i;
    }
}
