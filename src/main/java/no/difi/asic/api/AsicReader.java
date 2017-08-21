package no.difi.asic.api;

import com.google.common.io.ByteStreams;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author erlend
 */
public interface AsicReader extends Closeable {

    String next() throws IOException;

    InputStream getContent() throws IOException;

    default void writeTo(File file) throws IOException {
        writeTo(file.toPath());
    }

    default void writeTo(Path path) throws IOException {
        try (OutputStream outputStream = Files.newOutputStream(path)) {
            writeTo(outputStream);
        }
    }

    default void writeTo(OutputStream outputStream) throws IOException {
        try (InputStream inputStream = getContent()) {
            ByteStreams.copy(inputStream, outputStream);
        }
    }
}