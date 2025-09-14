package org.jabref.http.dto.zotero;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ZoteroSaveItemsRequest {
    @JsonProperty("items")
    private List<ZoteroItem> items;

    public ZoteroSaveItemsRequest() {}

    public ZoteroSaveItemsRequest(List<ZoteroItem> items) {
        this.items = items;
    }

    public List<ZoteroItem> getItems() { return items; }
    public void setItems(List<ZoteroItem> items) { this.items = items; }
}
