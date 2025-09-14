package org.jabref.http.dto.zotero;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

public class ZoteroItem {
    @JsonProperty("id")
    private String id;

    @JsonProperty("itemType")
    private String itemType;

    @JsonProperty("title")
    private String title;

    @JsonProperty("url")
    private String url;

    @JsonProperty("accessDate")
    private String accessDate;

    @JsonProperty("attachments")
    private List<ZoteroAttachment> attachments;

    private final Map<String, Object> additionalFields = new HashMap<>();

    @JsonAnyGetter
    public Map<String, Object> getAdditionalFields() {
        return additionalFields;
    }

    @JsonAnySetter
    public void setAdditionalField(String key, Object value) {
        this.additionalFields.put(key, value);
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getAccessDate() { return accessDate; }
    public void setAccessDate(String accessDate) { this.accessDate = accessDate; }

    public List<ZoteroAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<ZoteroAttachment> attachments) { this.attachments = attachments; }
}
