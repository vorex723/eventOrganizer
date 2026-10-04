package com.mazurek.eventOrganizer.config.seed;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Tiny, real downloadable demo documents, generated without filesystem side effects. */
final class LocalSeedFiles {
    private LocalSeedFiles() {}

    static byte[] pdf(String title) {
        String text = title.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
        String stream = "BT /F1 18 Tf 50 760 Td (" + text + ") Tj ET\n";
        List<String> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>",
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
                "<< /Length " + ascii(stream).length + " >>\nstream\n" + stream + "endstream");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.writeBytes(ascii("%PDF-1.4\n"));
        List<Integer> offsets = new ArrayList<>();
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(output.size());
            output.writeBytes(ascii((index + 1) + " 0 obj\n" + objects.get(index) + "\nendobj\n"));
        }
        int xref = output.size();
        output.writeBytes(ascii("xref\n0 6\n0000000000 65535 f \n"));
        offsets.forEach(offset -> output.writeBytes(ascii(String.format(Locale.ROOT, "%010d 00000 n \n", offset))));
        output.writeBytes(ascii("trailer\n<< /Size 6 /Root 1 0 R >>\nstartxref\n" + xref + "\n%%EOF\n"));
        return output.toByteArray();
    }

    static byte[] png() {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                image.setRGB(x, y, (x < 16 ^ y < 16) ? 0x2980b9 : 0xf1c40f);
            }
        }
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            if (!ImageIO.write(image, "png", output)) {
                throw new IllegalStateException("PNG writer is unavailable");
            }
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot create local demo PNG", exception);
        }
    }

    private static byte[] ascii(String value) {
        return value.getBytes(StandardCharsets.US_ASCII);
    }
}
