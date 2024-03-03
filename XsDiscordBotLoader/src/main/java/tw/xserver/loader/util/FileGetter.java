package tw.xserver.loader.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class FileGetter {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileGetter.class);
    private final File FOLDER;
    private final Class<?> CLASS;

    public FileGetter(File folder, Class<?> clazz) {
        this.FOLDER = folder;
        this.CLASS = clazz;

        if (FOLDER.mkdir()) {
            LOGGER.debug("folder created: {}", FOLDER.getAbsolutePath());
        }
    }

    @Nullable
    public Map<String, Object> readMap(String fileName) throws IOException {
        try {
            File file = checkFileAvailable(fileName);
            LOGGER.info("load " + file.getPath());
            return readMap(file.toPath());
        } catch (IOException e) {
            LOGGER.error("read resource failed: {}", e.getMessage());
            throw e;
        }
    }

    @Nullable
    public Map<String, Object> readMap(Path filePath) throws IOException {
        try {
            return new Yaml().load(Files.newInputStream(filePath));
        } catch (IOException e) {
            LOGGER.error("error on loading yaml file: {}", e.getMessage());
            throw e;
        }
    }

    @Nullable
    public InputStream readInputStream(String fileName) throws IOException {
        try {
            File file = checkFileAvailable(fileName);
            LOGGER.info("loaded " + file.getPath());
            return Files.newInputStream(file.toPath());
        } catch (IOException e) {
            LOGGER.error("read resource failed: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * same path of source and dest
     *
     * @param sourceFilePath ex. "/info.yml"
     */
    public File exportResource(String sourceFilePath) throws IOException {
        return exportResource(sourceFilePath, sourceFilePath);
    }

    /**
     * @param sourceFilePath ex. "/info.yml"
     * @param outputPath     ex."/data/info.yml"
     */
    @Nullable
    public File exportResource(String sourceFilePath, String outputPath) throws IOException {
        try (InputStream fileInJar = getResource(sourceFilePath)) {
            if (fileInJar == null) {
                LOGGER.error("cannot find resource: {}", sourceFilePath);
                throw new FileNotFoundException();
            }

            Files.copy(fileInJar, Paths.get(FOLDER.getPath() + '/' + outputPath), StandardCopyOption.REPLACE_EXISTING);
            return new File(FOLDER.getPath() + '/' + outputPath);
        }
    }

    /**
     * @param sourceFilePath ex. "/info.yml"
     */
    @Nullable
    public InputStream getResource(String sourceFilePath) {
        return CLASS.getResourceAsStream(sourceFilePath);
    }

    /**
     * @param path ex. "/lang/"
     */
    public String[] getResources(String path) throws IOException {
        // source-code https://bingdoal.github.io/backend/2021/04/java-read-file-in-jar/

        if (path.startsWith("/")) path = path.substring(1);

        List<String> filenames = new ArrayList<>();
        URL jar = CLASS.getProtectionDomain().getCodeSource().getLocation();
        ZipInputStream zip = new ZipInputStream(jar.openStream());
        ZipEntry ze;
        while ((ze = zip.getNextEntry()) != null) {
            String entryName = ze.getName();

            if (entryName.startsWith(path) && !entryName.endsWith("/")) {
                filenames.add(entryName.substring(path.length()));
            }
        }

        return filenames.toArray(new String[0]);
    }


    private File checkFileAvailable(String fileName) throws IOException {
        File file = new File(FOLDER.getPath() + '/' + fileName);

        if (!file.exists()) {
            LOGGER.info("create default file {}", file.getAbsolutePath());
            return exportResource(fileName);
        }

        return file;
    }
}
