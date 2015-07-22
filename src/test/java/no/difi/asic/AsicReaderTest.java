package no.difi.asic;

import org.apache.commons.io.IOUtils;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class AsicReaderTest {

    private AsicReaderFactory asicReaderFactory = AsicReaderFactory.newFactory();
    private AsicWriterFactory asicWriterFactory = AsicWriterFactory.newFactory();
    private SignatureHelper signatureHelper = new SignatureHelper(getClass().getResourceAsStream("/kontaktinfo-client-test.jks"), "changeit", null, "changeit");

    private String fileContent1 = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam arcu eros, fermentum vel molestie ut, sagittis vel velit.";
    private String fileContent2 = "Fusce eu risus ipsum. Sed mattis laoreet justo. Fusce nisi magna, posuere ac placerat tincidunt, dignissim non lacus.";

    @Test
    public void writeAndReadSimpleContainer() throws IOException {
        ByteArrayOutputStream containerOutput = new ByteArrayOutputStream();

        asicWriterFactory.newContainer(containerOutput)
                .add(new ByteArrayInputStream(fileContent1.getBytes()), "content1.txt", "text/plain")
                .add(new ByteArrayInputStream(fileContent2.getBytes()), "content2.txt", "text/plain")
                .sign(signatureHelper);

        AsicReader asicReader = asicReaderFactory.open(new ByteArrayInputStream(containerOutput.toByteArray()));

        ByteArrayOutputStream fileStream;
        {
            assertEquals("content1.txt", asicReader.getNextFile());

            fileStream = new ByteArrayOutputStream();
            asicReader.writeFile(fileStream);
            assertEquals(fileContent1, fileStream.toString());
        }

        {
            assertEquals("content2.txt", asicReader.getNextFile());

            fileStream = new ByteArrayOutputStream();
            asicReader.writeFile(fileStream);
            assertEquals(fileContent2, fileStream.toString());
        }

        // To be removed at a later state.
        assertEquals("META-INF/asicmanifest.xml", asicReader.getNextFile());
        asicReader.getNextFile(); // Signature file

        assertNull(asicReader.getNextFile());

        asicReader.close();

    }

    @Test
    public void writeAndReadSimpleFileContainer() throws IOException {
        File tmpDir = new File(System.getProperty("java.io.tmpdir"));

        File file = new File(tmpDir, "asic-reader-sample.zip");

        asicWriterFactory.newContainer(file)
                .add(new ByteArrayInputStream(fileContent1.getBytes()), "content1.txt", "text/plain")
                .add(new ByteArrayInputStream(fileContent2.getBytes()), "content2.txt", "text/plain")
                .sign(signatureHelper);

        AsicReader asicReader = asicReaderFactory.open(file);

        File contentFile;
        String filename;
        ByteArrayOutputStream fileStream;
        {
            filename = asicReader.getNextFile();
            assertEquals("content1.txt", filename);

            contentFile = new File(tmpDir, "asic-" + filename);
            asicReader.writeFile(contentFile);

            fileStream = new ByteArrayOutputStream();
            IOUtils.copy(Files.newInputStream(contentFile.toPath()), fileStream);
            assertEquals(fileContent1, fileStream.toString());

            Files.delete(contentFile.toPath());
        }

        {
            filename = asicReader.getNextFile();
            assertEquals("content2.txt", filename);

            contentFile = new File(tmpDir, "asic-" + filename);
            asicReader.writeFile(contentFile);

            fileStream = new ByteArrayOutputStream();
            IOUtils.copy(Files.newInputStream(contentFile.toPath()), fileStream);
            assertEquals(fileContent2, fileStream.toString());

            Files.delete(contentFile.toPath());
        }

        // To be removed at a later state.
        assertEquals("META-INF/asicmanifest.xml", asicReader.getNextFile());
        asicReader.getNextFile(); // Signature file

        assertNull(asicReader.getNextFile());

        asicReader.close();

        Files.delete(file.toPath());
    }

}