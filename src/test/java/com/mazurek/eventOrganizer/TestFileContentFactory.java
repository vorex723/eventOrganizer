package com.mazurek.eventOrganizer;

import java.nio.charset.StandardCharsets;

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
        return zipHeader(); // DOCX is a ZIP file
    }

    public static byte[] ppt() {
        // Old binary PPT uses same CFB header as DOC
        return doc();
    }

    public static byte[] pptx() {

        return docx();
        //return zipHeader();
    }

    public static byte[] odt() {
        return zipHeader(); // ODT is a ZIP package
    }

    public static byte[] xls() {
        return doc(); // Old binary XLS = CFB format
    }

    public static byte[] xlsx() {
        return zipHeader();
    }

    public static byte[] mp4() {
        // MP4 'ftyp' box signature
        return new byte[] {0x00, 0x00, 0x00, 0x18, 0x66, 0x74, 0x79, 0x70};
    }

    public static byte[] avi() {
        // AVI = RIFF container
        return new byte[] {'R', 'I', 'F', 'F', 0x00, 0x00, 0x00, 0x00, 'A', 'V', 'I', ' '};
    }

    private static byte[] zipHeader() {

        return new byte[] {
                // Local File Header (start of ZIP)
                (byte)0x50, (byte)0x4B, (byte)0x03, (byte)0x04, // PK\x03\x04
                0x14, 0x00, // Version needed to extract
                0x00, 0x00, // General purpose bit flag
                0x00, 0x00, // Compression method (0 = no compression)
                0x00, 0x00, // File last mod time
                0x00, 0x00, // File last mod date
                0x00, 0x00, 0x00, 0x00, // CRC-32
                0x00, 0x00, 0x00, 0x00, // Compressed size
                0x00, 0x00, 0x00, 0x00, // Uncompressed size
                0x08, 0x00, // File name length
                0x00, 0x00, // Extra field length

                // File name: "test.txt"
                (byte)'t', (byte)'e', (byte)'s', (byte)'t',
                (byte)'.', (byte)'t', (byte)'x', (byte)'t',

                // Central Directory Header
                (byte)0x50, (byte)0x4B, (byte)0x01, (byte)0x02, // PK\x01\x02
                0x14, 0x00, // Version made by
                0x14, 0x00, // Version needed to extract
                0x00, 0x00, // General purpose bit flag
                0x00, 0x00, // Compression method
                0x00, 0x00, // File last mod time
                0x00, 0x00, // File last mod date
                0x00, 0x00, 0x00, 0x00, // CRC-32
                0x00, 0x00, 0x00, 0x00, // Compressed size
                0x00, 0x00, 0x00, 0x00, // Uncompressed size
                0x08, 0x00, // File name length
                0x00, 0x00, // Extra field length
                0x00, 0x00, // File comment length
                0x00, 0x00, // Disk number start
                0x00, 0x00, // Internal file attributes
                0x00, 0x00, 0x00, 0x00, // External file attributes
                0x00, 0x00, 0x00, 0x00, // Relative offset of local header

                // File name again
                (byte)'t', (byte)'e', (byte)'s', (byte)'t',
                (byte)'.', (byte)'t', (byte)'x', (byte)'t',

                // End of Central Directory (EOCD)
                (byte)0x50, (byte)0x4B, (byte)0x05, (byte)0x06, // PK\x05\x06
                0x00, 0x00, // Number of this disk
                0x00, 0x00, // Disk where central directory starts
                0x01, 0x00, // Number of central directory records on this disk
                0x01, 0x00, // Total number of central directory records
                0x2E, 0x00, 0x00, 0x00, // Size of central directory (46 bytes)
                0x1E, 0x00, 0x00, 0x00, // Offset of start of central directory
                0x00, 0x00 // ZIP file comment length
        };

    }
}
