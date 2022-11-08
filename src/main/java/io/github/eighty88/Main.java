package io.github.eighty88;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author eight_y_88
 * 2022/06/04
 */
@RequiredArgsConstructor
public class Main {
    @Getter
    private static final Logger logger = LoggerFactory.getLogger("CF-DL");

    private final File manifest;

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        File file;

        if (new UrlValidator().isValid(args[0])) {
            file = new File(FilenameUtils.getName(args[0]));
             if (!new Downloader(args[0]).download(file)) {
                 logger.error("File Not Found.");
                 return;
             }
        } else {
            file = new File(args[0]);
        }

        if (file.getName().contains(".zip")) {
            File output = new File(FilenameUtils.getBaseName(file.getName()));
            Utils.extractZip(file, output);
            file = output;
        }

        if (file.isDirectory()) {
            file = new File(file, "manifest.json");
            if (!file.exists()) {
                logger.error("manifest.json Not Found.");
                return;
            }
        } else if (!file.getName().equalsIgnoreCase("manifest.json")) {
            logger.error("manifest.json Not Found.");
            return;
        }

        File temp = new File(file.getParentFile(), "overrides");
        if (temp.exists()) {
            File[] files = temp.listFiles();
            if (files != null) {
                Arrays.stream(files).forEach(f -> {
                    try {
                        Files.move(f.toPath(), new File(temp.getParentFile(), f.getName()).toPath());
                    } catch (IOException e) {
                        logger.error("Failed to move.");
                    }
                });
            }

            if (temp.delete()) {
                logger.debug("Delete overrides.");
            }
        }

        new Main(file).downloadFiles();
    }

    public void downloadFiles() {
        Manifest data = Manifest.loadJson(manifest);
        if (data == null) {
            return;
        }

        Manifest.ModLoaderObject forge = data.minecraft.modLoaders.stream().filter(Manifest.ModLoaderObject::isPrimary).toList().get(0);
        File forgeJar = downloadForge(data.minecraft.version, forge.id.split("-")[1]);
        installForge(forgeJar);

        for (Manifest.ModObject object : data.files) {
            executor.submit(() -> {
                File mods = new File(manifest.getParentFile(), "mods");
                if (mods.mkdir()) {
                    logger.debug("Create mods folder");
                }
                new Downloader(Utils.getCurseModUrl(object.id, object.file)).download(mods);
            });
        }

        if (Utils.await(executor)) {
            logger.error("Timed out");
        }
    }

    private File downloadForge(String version, String forgeVersion) {
        String url = Utils.FORGE_URL.replace("${version}", version).replace("${forge-version}", forgeVersion);
        File file = new File(manifest.getParentFile(), FilenameUtils.getName(url));

        new Downloader(url).download(file);

        return file;
    }

    private void installForge(File forgeJar) {
        String temp;
        String javaLibraryPath = System.getProperty("java.home");

        if ("windows".equals(Utils.getPlatformName())) {
            temp = javaLibraryPath + "\\bin\\java.exe";
        } else {
            temp = javaLibraryPath + "/bin/java";
        }

        String[] args = new String[] {temp, "-jar", forgeJar.getAbsolutePath(), "--installServer"};

        ProcessBuilder builder = new ProcessBuilder(args);
        builder.redirectErrorStream(true);
        builder.directory(manifest.getParentFile());
        try {
            Process process = builder.start();

            InputStreamReader is = new InputStreamReader(process.getInputStream());
            BufferedReader br = new BufferedReader(is);
            String line;
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();
        } catch (IOException | InterruptedException e) {
            logger.error("Can't install Forge (" + e + ")");
        }
    }
}