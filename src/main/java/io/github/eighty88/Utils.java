package io.github.eighty88;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {
    public static final String API_URL = "https://www.curseforge.com/api/v1/mods/${id}/files/${file}/download";
    // public static final String PROJECT_URL = "https://www.curseforge.com/projects/";
    public static final String FORGE_URL = "https://maven.minecraftforge.net/net/minecraftforge/forge/${version}-${forge-version}/forge-${version}-${forge-version}-installer.jar";

    public static String getCurseModUrl(int id, int file) {
        return getRedirectUrl(API_URL.replace("${id}", String.valueOf(id)).replace("${file}", String.valueOf(file)));
        // return getRedirectUrl(getRedirectUrl(PROJECT_URL + id) + "/download/" + file + "/file");
    }

    private static String getRedirectUrl(String url) {
        try (Response response = new OkHttpClient.Builder().addNetworkInterceptor(chain -> chain.proceed(chain.request())).build().newCall(new Request.Builder().url(url).build()).execute()) {
            return response.request().url().toString();
        } catch (IOException e) {
            Main.getLogger().error("Get URL failed. ({})", String.valueOf(e));
            return "";
        }
    }

    public static boolean await(ExecutorService service) {
        service.shutdown();
        try {
            if (service.awaitTermination(60, TimeUnit.SECONDS)) {
                service.shutdownNow();
                return !service.awaitTermination(60, TimeUnit.SECONDS);
            }
        } catch (InterruptedException e) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }
        return true;
    }

    public static String alignNumber(String str, int length, boolean insert) {
        if (str.length() == length) {
            return str;
        }

        if (str.length() < length) {
            StringBuilder inputBuilder = new StringBuilder(str);
            for (int i = 0; i <= length - inputBuilder.length(); i++) {
                if (insert) {
                    inputBuilder.insert(0, " ");
                } else {
                    inputBuilder.append(" ");
                }
            }
            str = inputBuilder.toString();
            return str;
        } else {
            int i = (length * 2) / 3;
            return str.substring(0, i) + "..." + str.substring(str.length() - (length - (i + 3)));
        }
    }

    public static void extractZip(File input, File output) {
        Logger logger = Main.getLogger();
        try (ZipInputStream is = new ZipInputStream(new FileInputStream(input))) {
            if (!output.exists()) {
                if (!output.mkdir()) {
                    logger.error("Could not create directory.");
                    return;
                }
            }

            ZipEntry entry;
            int length;
            byte[] buffer = new byte[1024];
            while ((entry = is.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    File out = new File(output.getAbsolutePath(), entry.getName());
                    if (!out.exists())
                        if (out.mkdirs())
                            continue;
                }
                File out = new File(output.getAbsolutePath(), entry.getName());
                FileOutputStream os = new FileOutputStream(out);
                if (new File(out.getParent()).mkdirs()) {
                    logger.debug(out.getParentFile().getAbsolutePath());
                }

                logger.info("Unzipping to {}", alignNumber(out.getAbsolutePath(), 80, false));

                while ((length = is.read(buffer)) > 0) {
                    os.write(buffer, 0, length);
                }

                os.close();
                is.closeEntry();
            }
        } catch (IOException e) {
            logger.error("File Not Found. ({})", String.valueOf(e));
        }
    }

    public static String getPlatformName() {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Mac") || osName.startsWith("Darwin"))
            return "osx";
        if (osName.toLowerCase().contains("windows"))
            return "windows";
        return "linux";
    }
}
