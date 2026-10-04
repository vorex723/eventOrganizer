package com.mazurek.eventOrganizer.config.seed;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class LocalSeedFilesUnitTest {
    @Test
    void pdfHasSinglePageVisibleTextAndCorrectCrossReferenceOffsets() {
        String pdf = new String(LocalSeedFiles.pdf("Demo agenda (1)"), StandardCharsets.US_ASCII);
        assertThat(pdf).startsWith("%PDF-1.4").endsWith("%%EOF\n").contains("/Count 1", "(Demo agenda \\(1\\)) Tj");
        var entries = Pattern.compile("(\\d{10}) 00000 n").matcher(pdf);
        int object = 1;
        while (entries.find()) {
            assertThat(pdf.substring(Integer.parseInt(entries.group(1)))).startsWith(object++ + " 0 obj");
        }
        assertThat(object).isEqualTo(6);
        String offset = pdf.substring(pdf.indexOf("startxref\n") + "startxref\n".length()).split("\n")[0];
        assertThat(pdf.substring(Integer.parseInt(offset))).startsWith("xref\n");
    }

    @Test
    void pngIsActuallyDecodable() throws Exception {
        var image = ImageIO.read(new ByteArrayInputStream(LocalSeedFiles.png()));
        assertThat(image.getWidth()).isEqualTo(32);
        assertThat(image.getHeight()).isEqualTo(32);
    }
}
