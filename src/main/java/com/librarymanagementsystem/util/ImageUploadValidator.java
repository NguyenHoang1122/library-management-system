package com.librarymanagementsystem.util;

import org.springframework.web.multipart.MultipartFile;
import java.util.List;

public class ImageUploadValidator {

    private static final List<String> ALLOWED_MIME_TYPES = List.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg",
            "jpeg",
            "png",
            "gif",
            "webp"
    );

    public static void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new RuntimeException("Tệp tải lên không được để trống");
        }

        // 1. Kiểm tra MIME type
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new RuntimeException("Định dạng tệp không hợp lệ. Chỉ chấp nhận ảnh JPEG, PNG, GIF, WEBP");
        }

        // 2. Kiểm tra phần mở rộng mở rộng (extension)
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new RuntimeException("Tên tệp không hợp lệ");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new RuntimeException("Đuôi mở rộng không hợp lệ. Chỉ chấp nhận .jpg, .jpeg, .png, .gif, .webp");
        }
    }
}
