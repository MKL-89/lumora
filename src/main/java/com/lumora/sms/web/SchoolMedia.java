package com.lumora.sms.web;

import com.lumora.sms.exception.BusinessRuleViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * The school's own pictures: its logo and the photographs shown on the sign-in
 * page, dashboards and module headers. Exposed to Thymeleaf as {@code @schoolMedia}.
 *
 * <p>Every slot except the logo ships with a bundled default photo, so a fresh
 * install looks complete. When a school uploads its own picture it is written to
 * {@code app.media.dir} under a fixed name chosen here, never one supplied by the
 * browser, and it replaces the default until the school resets it.
 */
@Slf4j
@Component("schoolMedia")
public class SchoolMedia {

    /** The places a picture appears. The key is what URLs and templates use. */
    public enum Slot {
        LOGO("logo", "School logo",
             "Shown beside the school name in the sidebar and on the sign-in page. A square image works best.", false),
        LOGIN("login", "Sign-in page photo",
              "The large picture beside the sign-in form. Students or the school compound work well.", true),
        BANNER("banner", "Dashboard banner",
               "Across the top of every staff dashboard. A wide photo of the school.", true),
        PORTAL("portal", "Student and parent portal banner",
               "Across the top of the student and parent portals.", true),
        ACADEMICS("academics", "Academics banner",
                  "Across the top of the examinations and results pages. A classroom works well.", true),
        LIBRARY("library", "Library banner",
                "Across the top of the circulation desk and catalogue.", true);

        public final String key;
        public final String label;
        public final String hint;
        public final boolean hasDefault;

        Slot(String key, String label, String hint, boolean hasDefault) {
            this.key = key;
            this.label = label;
            this.hint = hint;
            this.hasDefault = hasDefault;
        }

        public String getKey() { return key; }
        public String getLabel() { return label; }
        public String getHint() { return hint; }
        public boolean isHasDefault() { return hasDefault; }

        public static Optional<Slot> of(String key) {
            return Arrays.stream(values()).filter(s -> s.key.equalsIgnoreCase(key)).findFirst();
        }
    }

    public static final long MAX_BYTES = 5L * 1024 * 1024;

    private static final List<String> EXTENSIONS = List.of("jpg", "png", "webp");

    private final Path dir;

    public SchoolMedia(@Value("${app.media.dir:./lumora-data/media}") String dir) {
        this.dir = Paths.get(dir).toAbsolutePath().normalize();
    }

    public List<Slot> slots() {
        return List.of(Slot.values());
    }

    /** True when the school has uploaded its own picture for this slot. */
    public boolean custom(String key) {
        return Slot.of(key).flatMap(this::customFile).isPresent();
    }

    /** True when there is anything to show: an upload, or a bundled default. */
    public boolean available(String key) {
        return Slot.of(key).map(s -> s.hasDefault || customFile(s).isPresent()).orElse(false);
    }

    /**
     * Changes whenever the picture changes, so pages can append it to the image URL
     * and browsers fetch the new picture instead of a cached old one.
     */
    public long version(String key) {
        return Slot.of(key).flatMap(this::customFile).map(p -> {
            try {
                return Files.getLastModifiedTime(p).toMillis();
            } catch (IOException e) {
                return 0L;
            }
        }).orElse(0L);
    }

    /** The picture to serve for a slot, with its content type, if there is one. */
    public Optional<Image> load(String key) {
        Optional<Slot> slot = Slot.of(key);
        if (slot.isEmpty()) {
            return Optional.empty();
        }
        Optional<Path> file = customFile(slot.get());
        if (file.isPresent()) {
            return Optional.of(new Image(new FileSystemResource(file.get()), typeOf(file.get())));
        }
        if (slot.get().hasDefault) {
            Resource bundled = new ClassPathResource("static/images/defaults/" + slot.get().key + ".jpg");
            if (bundled.exists()) {
                return Optional.of(new Image(bundled, MediaType.IMAGE_JPEG));
            }
        }
        return Optional.empty();
    }

    /**
     * Saves an upload as the school's picture for a slot. The format is decided by
     * the file's own bytes, not its name or the browser's claimed type, so only real
     * JPEG, PNG or WebP images are ever stored or served.
     */
    public void store(String key, MultipartFile upload) {
        Slot slot = Slot.of(key).orElseThrow(() -> new BusinessRuleViolationException("Unknown picture slot."));
        if (upload == null || upload.isEmpty()) {
            throw new BusinessRuleViolationException("Choose a picture to upload first.");
        }
        if (upload.getSize() > MAX_BYTES) {
            throw new BusinessRuleViolationException("That picture is larger than 5 MB. Please use a smaller one.");
        }
        byte[] head = new byte[12];
        int read;
        try (InputStream in = upload.getInputStream()) {
            read = in.readNBytes(head, 0, head.length);
        } catch (IOException e) {
            throw new BusinessRuleViolationException("The picture could not be read. Please try again.");
        }
        String ext = detect(head, read)
                .orElseThrow(() -> new BusinessRuleViolationException(
                        "Only JPEG, PNG or WebP pictures can be used."));
        try {
            Files.createDirectories(dir);
            Path temp = Files.createTempFile(dir, slot.key, ".upload");
            try {
                upload.transferTo(temp);
                removeCustom(slot);
                Files.move(temp, dir.resolve(slot.key + "." + ext), StandardCopyOption.REPLACE_EXISTING);
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            log.error("Could not save picture for slot {}", slot.key, e);
            throw new BusinessRuleViolationException("The picture could not be saved. Please try again.");
        }
    }

    /** Removes the school's picture so the bundled default (or no logo) is used again. */
    public void reset(String key) {
        Slot slot = Slot.of(key).orElseThrow(() -> new BusinessRuleViolationException("Unknown picture slot."));
        try {
            removeCustom(slot);
        } catch (IOException e) {
            log.error("Could not remove picture for slot {}", slot.key, e);
            throw new BusinessRuleViolationException("The picture could not be removed. Please try again.");
        }
    }

    static Optional<String> detect(byte[] b, int n) {
        if (n >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return Optional.of("jpg");
        }
        if (n >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return Optional.of("png");
        }
        if (n >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return Optional.of("webp");
        }
        return Optional.empty();
    }

    private Optional<Path> customFile(Slot slot) {
        return EXTENSIONS.stream().map(ext -> dir.resolve(slot.key + "." + ext)).filter(Files::isRegularFile).findFirst();
    }

    private void removeCustom(Slot slot) throws IOException {
        for (String ext : EXTENSIONS) {
            Files.deleteIfExists(dir.resolve(slot.key + "." + ext));
        }
    }

    private static MediaType typeOf(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (name.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        return MediaType.IMAGE_JPEG;
    }

    public record Image(Resource resource, MediaType type) { }
}
