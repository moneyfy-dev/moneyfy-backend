package com.referidos.app.segurosref.helpers;

public class ImageHelper {

    public static boolean validatePictureSize(byte[] pictureBytes) {
        return 204800 >= pictureBytes.length; // 200KB
    }

    public static boolean validatePictureSize(long pictureBytes) {
        return 204800 >= pictureBytes; // 200KB
    }

}
