package org.jabref.http.server;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import jakarta.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Map;
import java.util.HashMap;

import org.jabref.http.dto.zotero.ZoteroSaveItemsRequest;

@Path("/connector")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ZoteroConnectorEndpoint {

    @POST
    @Path("/saveItems")
    public Response saveItems(ZoteroSaveItemsRequest request) {
        try {
            // Process the saveItems request
            System.out.println("Saving items: " + request.getItems().size());

            // Simulate Zotero processing
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("items", request.getItems());

            return Response.ok()
                    .header("X-Zotero-Version", "6.0.0")
                    .entity(response)
                    .build();

        } catch (Exception e) {
            return Response.status(500)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/saveAttachment")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response saveAttachment(
            @FormParam("md5") String md5,
            @FormParam("filename") String filename,
            @FormParam("filesize") String filesize,
            @FormParam("mtime") String mtime,
            @FormParam("contentType") String contentType,
            @FormParam("charset") String charset,
            @FormParam("binary-data") InputStream binaryData) {

        try {
            // Process attachment upload
            System.out.println("Uploading attachment: " + filename);
            System.out.println("Size: " + filesize + " bytes");
            System.out.println("MD5: " + md5);

            // Read the binary data
            byte[] data = binaryData.readAllBytes();
            System.out.println("Received " + data.length + " bytes");

            // Simulate successful upload
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("filename", filename);
            response.put("size", data.length);

            return Response.ok()
                    .header("X-Zotero-Version", "6.0.0")
                    .entity(response)
                    .build();

        } catch (Exception e) {
            return Response.status(500)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @POST
    @Path("/saveSingleFile")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response saveSingleFile(SingleFileRequest request) {
        try {
            // Process SingleFile snapshot
            System.out.println("Saving SingleFile snapshot for: " + request.getTitle());
            System.out.println("Content length: " +
                    (request.getSnapshotContent() != null ? request.getSnapshotContent().length() : 0));

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("sessionID", request.getSessionID());

            return Response.ok()
                    .header("X-Zotero-Version", "6.0.0")
                    .entity(response)
                    .build();

        } catch (Exception e) {
            return Response.status(500)
                    .entity(Map.of("error", e.getMessage()))
                    .build();
        }
    }

    @GET
    @Path("/ping")
    public Response ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "ok");
        response.put("version", "6.0.0");

        return Response.ok()
                .header("X-Zotero-Version", "6.0.0")
                .entity(response)
                .build();
    }

    @POST
    @Path("/getSelectedCollection")
    public Response getSelectedCollection(JsonObject request) {
        Map<String, Object> response = new HashMap<>();
        response.put("filesEditable", true);
        response.put("collectionName", "My Library");

        return Response.ok()
                .header("X-Zotero-Version", "6.0.0")
                .entity(response)
                .build();
    }

    @POST
    @Path("/sessionProgress")
    public Response sessionProgress(SessionProgressRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("sessionID", request.getSessionID());
        response.put("progress", 100);
        response.put("items", new Object[0]);

        return Response.ok()
                .header("X-Zotero-Version", "6.0.0")
                .entity(response)
                .build();
    }
}
