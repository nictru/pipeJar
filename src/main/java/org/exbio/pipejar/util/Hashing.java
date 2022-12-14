package org.exbio.pipejar.util;

import org.exbio.pipejar.util.Comparators.FileComparator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;

public class Hashing {
    public static String hash(String text) {
        MessageDigest digest = getMessageDigest();
        byte[] hashBytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
        return bytesToString(hashBytes);
    }

    public static String hashFile(File file) throws IOException {
        return hashFile(file, pathname -> pathname.isFile() || !pathname.getName().equals("__pycache__"));
    }

    public static String hashFile(File file, FileFilter filter) throws IOException {
        Vector<FileInputStream> fileStreams = new Vector<>();
        collectInputStreams(file, fileStreams, filter);

        MessageDigest md = getMessageDigest();
        try (SequenceInputStream seqStream = new SequenceInputStream(fileStreams.elements());
             DigestInputStream stream = new DigestInputStream(seqStream, md)) {
            byte[] buffer = new byte[1024 * 8];
            while (stream.read(buffer) != -1) {
            }
        }

        return bytesToString(md.digest());
    }

    private static void collectInputStreams(File file, Collection<FileInputStream> foundStreams, FileFilter filter) throws FileNotFoundException {
        if (file.isFile()) {
            foundStreams.add(new FileInputStream(file));
        } else {
            File[] fileList = file.listFiles(filter);

            if (fileList != null) {
                Arrays.sort(fileList, new FileComparator());

                for (File f : fileList) {
                    collectInputStreams(f, foundStreams, filter);
                }
            }
        }
    }

    private static MessageDigest getMessageDigest() {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ignore) {
        }
        assert digest != null;

        return digest;
    }

    private static String bytesToString(byte[] bytes) {
        byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(StandardCharsets.UTF_8);
        byte[] hexChars = new byte[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }
}
