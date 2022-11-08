package io.github.eighty88;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import lombok.Getter;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Getter
public class Manifest {
    MinecraftObject minecraft;
    List<ModObject> files;

    public static Manifest loadJson(File file) {
        Gson gson = new Gson();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, Manifest.class);
        } catch (IOException e) {
            Main.getLogger().error("Can't load manifest.json");
        }
        return null;
    }

    @Getter
    public static class ModObject {
        @SerializedName("projectID")
        int id;

        @SerializedName("fileID")
        int file;

        boolean required;
    }

    @Getter
    public static class MinecraftObject {
        String version;

        List<ModLoaderObject> modLoaders;
    }

    @Getter
    public static class ModLoaderObject {
        String id;

        boolean primary;
    }
}
