package com.lumora.sms.web;

import com.lumora.sms.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * A school's uploaded pictures. Only genuine images are accepted, whatever the
 * file is called, and resetting brings the built-in photo back.
 */
@DisplayName("School photos")
class SchoolMediaTest {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 13};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};

    @TempDir Path dir;
    private SchoolMedia media;

    @BeforeEach
    void setUp() {
        media = new SchoolMedia(dir.toString());
    }

    @Test
    @DisplayName("serves the bundled photo until the school uploads its own")
    void defaultsFirst() {
        assertThat(media.custom("banner")).isFalse();
        assertThat(media.available("banner")).isTrue();
        assertThat(media.load("banner")).get().extracting(SchoolMedia.Image::type).isEqualTo(MediaType.IMAGE_JPEG);
    }

    @Test
    @DisplayName("has no logo until one is uploaded")
    void noDefaultLogo() {
        assertThat(media.available("logo")).isFalse();
        assertThat(media.load("logo")).isEmpty();
    }

    @Test
    @DisplayName("stores a PNG under a fixed name and serves it as PNG")
    void storesPng() {
        media.store("logo", new MockMultipartFile("file", "../../evil.jpg", "image/jpeg", PNG));

        assertThat(dir.resolve("logo.png")).exists();
        assertThat(media.custom("logo")).isTrue();
        assertThat(media.load("logo")).get().extracting(SchoolMedia.Image::type).isEqualTo(MediaType.IMAGE_PNG);
    }

    @Test
    @DisplayName("replaces an earlier upload of a different format")
    void replaces() {
        media.store("banner", new MockMultipartFile("file", "a.png", "image/png", PNG));
        media.store("banner", new MockMultipartFile("file", "b.jpg", "image/jpeg", JPEG));

        assertThat(dir.resolve("banner.png")).doesNotExist();
        assertThat(dir.resolve("banner.jpg")).exists();
    }

    @Test
    @DisplayName("refuses a file that is not really an image, whatever it is called")
    void refusesNonImage() {
        var fake = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "<html>hi</html>".getBytes());

        assertThatThrownBy(() -> media.store("banner", fake))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("JPEG, PNG or WebP");
        assertThat(media.custom("banner")).isFalse();
    }

    @Test
    @DisplayName("refuses pictures over 5 MB")
    void refusesLarge() {
        byte[] big = new byte[(int) SchoolMedia.MAX_BYTES + 1];
        System.arraycopy(JPEG, 0, big, 0, JPEG.length);

        assertThatThrownBy(() -> media.store("banner", new MockMultipartFile("file", "big.jpg", "image/jpeg", big)))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("5 MB");
    }

    @Test
    @DisplayName("refuses an unknown slot")
    void refusesUnknownSlot() {
        assertThatThrownBy(() -> media.store("../secrets", new MockMultipartFile("file", "a.png", "image/png", PNG)))
                .isInstanceOf(BusinessRuleViolationException.class);
        assertThat(media.load("../secrets")).isEmpty();
    }

    @Test
    @DisplayName("reset removes the upload and brings back the built-in photo")
    void reset() throws Exception {
        media.store("library", new MockMultipartFile("file", "l.png", "image/png", PNG));
        media.reset("library");

        try (var files = Files.list(dir)) {
            assertThat(files.filter(p -> p.getFileName().toString().startsWith("library"))).isEmpty();
        }
        assertThat(media.load("library")).get().extracting(SchoolMedia.Image::type).isEqualTo(MediaType.IMAGE_JPEG);
    }
}
