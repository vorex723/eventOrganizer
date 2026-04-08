package com.mazurek.eventOrganizer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class TestFileContentFactory {
    public static byte[] jpg() {
        return new byte[] {(byte)0xFF, (byte)0xD8, (byte)0xFF}; // JPEG SOI
    }

    public static byte[] jpeg() {
        return jpg(); // Same magic number
    }

    public static byte[] png() {
        return new byte[] {(byte)137, 80, 78, 71, 13, 10, 26, 10}; // PNG signature
    }

    public static byte[] pdf() {
        return "%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII);
    }

    public static byte[] doc() {
        // DOC (old binary) magic number (CFB file header)
        return new byte[] {(byte)0xD0, (byte)0xCF, 0x11, (byte)0xE0, (byte)0xA1, (byte)0xB1, 0x1A, (byte)0xE1};
    }

    public static byte[] docx() {
        return zipWithEntries("[Content_Types].xml", "_rels/.rels", "word/document.xml");
    }

    public static byte[] ppt() {
        // Old binary PPT uses same CFB header as DOC
        return doc();
    }

    public static byte[] pptx() {
        return zipWithEntries("[Content_Types].xml", "_rels/.rels", "ppt/presentation.xml");
    }

    public static byte[] odt() {
        return zipWithEntries("mimetype", "content.xml");
    }

    public static byte[] xls() {
        return doc(); // Old binary XLS = CFB format
    }

    public static byte[] xlsx() {
        return zipWithEntries("[Content_Types].xml", "_rels/.rels", "xl/workbook.xml");
    }

    public static byte[] mp4() {
        // MP4 'ftyp' box signature
        return new byte[] {0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70};
    }

    public static byte[] avi() {
        // AVI = RIFF container
        return new byte[] {'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00, 'A', 'V', 'I', ' '};
    }

    public static byte[] malwareContent(){
        return new byte[] {0x50, 0x4B, 0x03, 0x04};
    }

    private static byte[] zipWithEntries(String... entryNames) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)) {
                for (String entryName : entryNames) {
                    zipOutputStream.putNextEntry(new ZipEntry(entryName));
                    zipOutputStream.write(contentForEntry(entryName));
                    zipOutputStream.closeEntry();
                }
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create test ZIP content.", exception);
        }
    }

    private static byte[] contentForEntry(String entryName) {
        if ("mimetype".equals(entryName)) {
            return "application/vnd.oasis.opendocument.text".getBytes(StandardCharsets.UTF_8);
        }
        return "<xml/>".getBytes(StandardCharsets.UTF_8);
    }
}
