package com.mazurek.eventOrganizer.testData;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.TimeZone;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.mazurek.eventOrganizer.testData.TestConstants.TimeConstants;
import static org.assertj.core.api.Assertions.assertThat;

@Isolated("Tests temporarily change the JVM default time zone")
@Execution(ExecutionMode.SAME_THREAD)
@DisplayName("Test file content factory unit tests:")
class TestFileContentFactoryUnitTest {
    @ParameterizedTest(name = "{0}: fresh deterministic bytes")
    @MethodSource("fileTypes")
    void whenGeneratingFileContentShouldReturnStableBytesInFreshBuffers(String name, Supplier<byte[]> content) {
        byte[] first = content.get();
        byte[] second = content.get();

        assertThat(first).isNotEmpty().containsExactly(second);
        assertThat(second).isNotSameAs(first);
        first[0] ^= 1;
        assertThat(content.get()).containsExactly(second);
    }

    @ParameterizedTest(name = "{0}: explicit ZIP timestamps and entries")
    @MethodSource("zipTypes")
    void whenGeneratingArchiveShouldSetDeterministicLocalTimestampForEveryEntry(
            String name, Supplier<byte[]> content, List<String> expectedEntries) throws IOException {
        try (var zip = new ZipInputStream(new ByteArrayInputStream(content.get()))) {
            var actualEntries = new java.util.ArrayList<String>();
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                actualEntries.add(entry.getName());
                assertThat(entry.getTimeLocal()).as(name + ": " + entry.getName())
                        .isEqualTo(TimeConstants.LOCAL_DATE_TIME_NOW);
                assertThat(zip.readAllBytes()).isNotEmpty();
            }
            assertThat(actualEntries).containsExactlyElementsOf(expectedEntries);
        }
    }

    @ParameterizedTest(name = "{0}: stable bytes in {2}")
    @MethodSource("zipTypesAndZones")
    @ResourceLock("default-time-zone")
    void whenJvmTimeZoneChangesShouldKeepArchiveBytesUnchanged(
            String name, Supplier<byte[]> content, String zone) {
        TimeZone previous = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
            byte[] baseline = content.get();
            TimeZone.setDefault(TimeZone.getTimeZone(zone));

            assertThat(content.get()).as(name + " in " + zone).containsExactly(baseline);
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    static Stream<Arguments> fileTypes() {
        return Stream.of(
                Arguments.of("JPG", (Supplier<byte[]>) TestFileContentFactory::jpg),
                Arguments.of("JPEG", (Supplier<byte[]>) TestFileContentFactory::jpeg),
                Arguments.of("PNG", (Supplier<byte[]>) TestFileContentFactory::png),
                Arguments.of("PDF", (Supplier<byte[]>) TestFileContentFactory::pdf),
                Arguments.of("DOC", (Supplier<byte[]>) TestFileContentFactory::doc),
                Arguments.of("DOCX", (Supplier<byte[]>) TestFileContentFactory::docx),
                Arguments.of("ODT", (Supplier<byte[]>) TestFileContentFactory::odt),
                Arguments.of("PPT", (Supplier<byte[]>) TestFileContentFactory::ppt),
                Arguments.of("PPTX", (Supplier<byte[]>) TestFileContentFactory::pptx),
                Arguments.of("XLS", (Supplier<byte[]>) TestFileContentFactory::xls),
                Arguments.of("XLSX", (Supplier<byte[]>) TestFileContentFactory::xlsx),
                Arguments.of("MP4", (Supplier<byte[]>) TestFileContentFactory::mp4),
                Arguments.of("AVI", (Supplier<byte[]>) TestFileContentFactory::avi)
        );
    }

    static Stream<Arguments> zipTypes() {
        return Stream.of(
                Arguments.of("DOCX", (Supplier<byte[]>) TestFileContentFactory::docx,
                        List.of("[Content_Types].xml", "_rels/.rels", "word/document.xml")),
                Arguments.of("PPTX", (Supplier<byte[]>) TestFileContentFactory::pptx,
                        List.of("[Content_Types].xml", "_rels/.rels", "ppt/presentation.xml")),
                Arguments.of("XLSX", (Supplier<byte[]>) TestFileContentFactory::xlsx,
                        List.of("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml")),
                Arguments.of("ODT", (Supplier<byte[]>) TestFileContentFactory::odt,
                        List.of("mimetype", "content.xml"))
        );
    }

    static Stream<Arguments> zipTypesAndZones() {
        return zipTypes().flatMap(type -> Stream.of("Europe/Warsaw", "America/New_York", "Pacific/Honolulu")
                .map(zone -> Arguments.of(type.get()[0], type.get()[1], zone)));
    }
}
