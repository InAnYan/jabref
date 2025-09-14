package org.jabref.http.dto.zotero;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ZoteroAttachment {
    @JsonProperty("id")
    private String id;

    @JsonProperty("title")
    private String title;

    @JsonProperty("url")
    private String url;

    @JsonProperty("mimeType")
    private String mimeType;

    @JsonProperty("parentItem")
    private String parentItem;

    @JsonProperty("snapshot")
    private Boolean snapshot;

    @JsonProperty("singleFile")
    private Boolean singleFile;

    @JsonProperty("isPrimary")
    private Boolean isPrimary;

    @JsonProperty("referrer")
    private String referrer;

    public ZoteroAttachment() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getParentItem() { return parentItem; }
    public void setParentItem(String parentItem) { this.parentItem = parentItem; }

    public Boolean getSnapshot() { return snapshot; }
    public void setSnapshot(Boolean snapshot) { this.snapshot = snapshot; }

    public Boolean getSingleFile() { return singleFile; }
    public void setSingleFile(Boolean singleFile) { this.singleFile = singleFile; }

    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean isPrimary) { this.isPrimary = isPrimary; }

    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
}
