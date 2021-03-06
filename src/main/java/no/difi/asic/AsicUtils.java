package no.difi.asic;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;

public class AsicUtils {

    private static Logger logger = LoggerFactory.getLogger(AsicUtils.class);

    /** The MIME type, which should be the very first entry in the container */
    public static final String MIMETYPE_ASICE = "application/vnd.etsi.asic-e+zip";

    static final Pattern PATTERN_CADES_MANIFEST = Pattern.compile("META-INF/asicmanifest(.*)\\.xml", Pattern.CASE_INSENSITIVE);
    static final Pattern PATTERN_CADES_SIGNATURE = Pattern.compile("META-INF/signature(.*)\\.p7s", Pattern.CASE_INSENSITIVE);
    static final Pattern PATTERN_XADES_SIGNATURES = Pattern.compile("META-INF/signatures(.*)\\.xml", Pattern.CASE_INSENSITIVE);

    static final Pattern PATTERN_EXTENSION_ASICE = Pattern.compile(".+\\.(asice|sce)", Pattern.CASE_INSENSITIVE);

    AsicUtils() {
        // No action
    }

    /**
     * Combine multiple containers to one container.
     *
     * OASIS OpenDocument manifest is regenerated if all source containers contains valid manifest.
     *
     * @param outputStream Stream for target container.
     * @param inputStreams Streams for source containers.
     */
    public static void combine(OutputStream outputStream, InputStream... inputStreams) throws IOException {
        // Statuses
        int manifestCounter = 0;
        int fileCounter = 0;
        boolean containsRootFile = false;

        // Open target container
        AsicOutputStream target = new AsicOutputStream(outputStream);

        // Prepare to combine OASIS OpenDocument Manifests
        OasisManifest oasisManifest = new OasisManifest(MimeType.forString(MIMETYPE_ASICE));

        for (InputStream inputStream : inputStreams) {
            // Open source container
            AsicInputStream source = new AsicInputStream(inputStream);

            // Read entries
            ZipEntry zipEntry;
            while ((zipEntry = source.getNextEntry()) != null) {
                if (PATTERN_CADES_MANIFEST.matcher(zipEntry.getName()).matches()) {
                    // Fetch content
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    ByteStreams.copy(source, byteArrayOutputStream);

                    // Read manifest
                    ManifestVerifier manifestVerifier = new ManifestVerifier(null);
                    CadesAsicManifest.extractAndVerify(byteArrayOutputStream.toString(), manifestVerifier);

                    // Make sure only on rootfile makes it to the source container
                    if (manifestVerifier.getAsicManifest().getRootfile() != null) {
                        if (containsRootFile)
                            throw new IllegalStateException("Multiple rootfiles is not allowed when combining containers.");
                        containsRootFile = true;
                    }

                    // Write manifest to container
                    target.putNextEntry(new ZipEntry(String.format("META-INF/asicmanifest%s.xml", ++manifestCounter)));
                    ByteStreams.copy(new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), target);
                } else if (PATTERN_XADES_SIGNATURES.matcher(zipEntry.getName()).matches()) {
                    // Copy content to target container
                    target.putNextEntry(new ZipEntry(String.format("META-INF/signatures%s.xml", ++manifestCounter)));
                    ByteStreams.copy(source, target);
                } else if (zipEntry.getName().equals("META-INF/manifest.xml")) {
                    // Fetch content
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    ByteStreams.copy(source, byteArrayOutputStream);

                    // Copy entries
                    oasisManifest.append(new OasisManifest(new ByteArrayInputStream(byteArrayOutputStream.toByteArray())));

                    // Nothing to write to target container
                    target.closeEntry();
                    continue;
                } else {
                    // Copy content to target container
                    target.putNextEntry(zipEntry);
                    ByteStreams.copy(source, target);

                    if (!zipEntry.getName().startsWith("META-INF/"))
                        fileCounter++;
                }

                source.closeEntry();
                target.closeEntry();
            }

            // Close source container
            source.close();
        }

        // Add manifest if it contains the same amount of files as the container.
        if (oasisManifest.size() == fileCounter + 1)
            target.writeZipEntry("META-INF/manifest.xml", oasisManifest.toBytes());

        // Close target container
        target.close();
    }

    public static MimeType detectMime(String filename) throws IOException {
        // Use Files to find content type
        String mimeType = Files.probeContentType(Paths.get(filename));

        // Use URLConnection to find content type
        if (mimeType == null) {
            logger.info("Unable to determine MIME type using Files.probeContentType(), trying URLConnection.getFileNameMap()");
            mimeType = URLConnection.getFileNameMap().getContentTypeFor(filename);
        }

        // Throw exception if content type is not detected
        if (mimeType == null) {
            throw new IllegalStateException(String.format("Unable to determine MIME type of %s", filename));
        }

        return MimeType.forString(mimeType);
    }

}