package com.foodzie.user_service.service;

import org.springframework.web.multipart.MultipartFile;

public interface CloudStorageService {

    /**
     * Uploads a file to cloud storage and returns the public URL.
     *
     * @param file      the multipart file to upload
     * @param folder    the destination folder/path in cloud storage
     * @param publicId  optional public ID for the asset (pass null to auto-generate)
     * @return the secure public URL of the uploaded asset
     */
    String upload(MultipartFile file, String folder, String publicId);

    /**
     * Deletes an asset from cloud storage by its public ID.
     *
     * @param publicId the public ID of the asset to delete
     */
    void delete(String publicId);
}
