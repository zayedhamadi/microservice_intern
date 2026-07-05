package user.service.Serivce;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Blob;
import java.sql.SQLException;
import java.util.Base64;


@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileService {

    private static final int PNG_MAGIC_BYTE_0 = 0x89;
    private static final int PNG_MAGIC_BYTE_1 = 0x50;
    private static final int PNG_MAGIC_BYTE_2 = 0x4E;
    private static final int PNG_MAGIC_BYTE_3 = 0x47;


    public byte[] decodeBase64(String base64) {
        if (base64 == null || base64.isBlank()) {
            return null;
        }

        try {
            String data = base64;
            if (data.contains(",")) {
                data = data.split(",", 2)[1];
            }
            return Base64.getDecoder().decode(data);
        } catch (IllegalArgumentException e) {
            log.warn("Erreur décodage base64 : {}", e.getMessage());
            return null;
        }
    }


    public String encodeToDataUri(byte[] bytes, String mimeType) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);
    }


    public String encodeImageToDataUri(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String mimeType = isPng(bytes) ? "image/png" : "image/jpeg";
        return encodeToDataUri(bytes, mimeType);
    }


    public String encodePdfToDataUri(byte[] bytes) {
        return encodeToDataUri(bytes, "application/pdf");
    }


    public String toDataUri(Object raw, String mimeType) {
        if (raw == null) return null;

        if (raw instanceof String s) {
            return s.isEmpty() ? null :
                    (s.startsWith("data:") ? s : "data:" + mimeType + ";base64," + s);
        }

        if (raw instanceof byte[] bytes) {
            return encodeToDataUri(bytes, mimeType);
        }

        if (raw instanceof Blob blob) {
            try {
                byte[] bytes = blob.getBytes(1, (int) blob.length());
                return encodeToDataUri(bytes, mimeType);
            } catch (SQLException e) {
                log.error("Erreur lecture Blob : {}", e.getMessage());
                return null;
            }
        }

        log.warn("Type inattendu pour fichier : {}", raw.getClass().getName());
        return null;
    }


    public String imageToDataUri(Object raw) {
        if (raw == null) return null;

        if (raw instanceof byte[] bytes) {
            return encodeImageToDataUri(bytes);
        }

        if (raw instanceof String s) {
            return s.isEmpty() ? null :
                    (s.startsWith("data:") ? s : "data:image/jpeg;base64," + s);
        }

        if (raw instanceof Blob blob) {
            try {
                byte[] bytes = blob.getBytes(1, (int) blob.length());
                return encodeImageToDataUri(bytes);
            } catch (SQLException e) {
                log.error("Erreur lecture Blob image : {}", e.getMessage());
                return null;
            }
        }

        return null;
    }


    public boolean isPng(byte[] bytes) {
        return bytes != null
                && bytes.length >= 4
                && (bytes[0] & 0xFF) == PNG_MAGIC_BYTE_0
                && (bytes[1] & 0xFF) == PNG_MAGIC_BYTE_1
                && (bytes[2] & 0xFF) == PNG_MAGIC_BYTE_2
                && (bytes[3] & 0xFF) == PNG_MAGIC_BYTE_3;
    }


    public boolean isValidFileSize(byte[] bytes, long maxSizeBytes) {
        return bytes != null && bytes.length <= maxSizeBytes;
    }

    
    public byte[] sanitizeFileData(byte[] bytes) {
        return (bytes != null && bytes.length > 0) ? bytes : null;
    }
}