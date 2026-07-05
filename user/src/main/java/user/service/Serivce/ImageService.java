package user.service.Serivce;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;

@Service
@Slf4j
public class ImageService {

    public String toBase64(byte[] bytes) {

        if (bytes == null || bytes.length == 0)
            return null;

        String mime =
                isPng(bytes)
                        ? "image/png"
                        : "image/jpeg";

        return "data:" + mime + ";base64,"
                + Base64.getEncoder()
                .encodeToString(bytes);
    }

    private boolean isPng(byte[] b) {
        return b.length >= 4
                && b[0] == (byte) 0x89
                && b[1] == (byte) 0x50
                && b[2] == (byte) 0x4E
                && b[3] == (byte) 0x47;
    }
}