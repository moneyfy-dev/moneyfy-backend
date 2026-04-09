package com.referidos.app.segurosref.helpers;

import org.springframework.web.multipart.MultipartFile;

public class ImageHelper {

    public static boolean validatePictureSize(byte[] pictureBytes) {
        return 204800 >= pictureBytes.length; // 200KB
    }

    public static boolean validatePictureSize(long pictureBytes) {
        return 204800 >= pictureBytes; // 200KB
    }

    public static boolean verifyImageFile(MultipartFile file) {
        if(file == null) {
            return false;
        }
        String contentType = file.getContentType();
        if(contentType == null || !contentType.startsWith("image/")
                || file.getSize() > 204800) { // 200KB
            return false;
        }
        return true;
    }

}
