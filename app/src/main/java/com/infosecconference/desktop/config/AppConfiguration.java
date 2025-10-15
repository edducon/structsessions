package com.infosecconference.desktop.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

/**
 * Lightweight configuration holder that reads {@code application.properties} from the classpath.
 */
public final class AppConfiguration {
    private static final String CONFIG_FILE = "application.properties";

    private final String databaseUrl;
    private final String databaseUser;
    private final String databasePassword;
    private final Path materialsRoot;
    private final Path imageRoot;
    private final Path styleGuidePath;

    private AppConfiguration(String databaseUrl,
                             String databaseUser,
                             String databasePassword,
                             Path materialsRoot,
                             Path imageRoot,
                             Path styleGuidePath) {
        this.databaseUrl = databaseUrl;
        this.databaseUser = databaseUser;
        this.databasePassword = databasePassword;
        this.materialsRoot = materialsRoot;
        this.imageRoot = imageRoot;
        this.styleGuidePath = styleGuidePath;
    }

    public static AppConfiguration load() throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = AppConfiguration.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (stream == null) {
                throw new IOException("Не найден файл конфигурации " + CONFIG_FILE);
            }
            properties.load(stream);
        }

        String dbUrl = requireProperty(properties, "db.url");
        String dbUser = requireProperty(properties, "db.user");
        String dbPassword = properties.getProperty("db.password", "");

        Path materialsRoot = resolvePath(requireProperty(properties, "materials.root"));
        Path imageRoot = resolvePath(requireProperty(properties, "images.root"));
        Path styleGuide = resolvePath(requireProperty(properties, "style.guide"));

        ensureDirectory(materialsRoot, "Каталог материалов");
        ensureDirectory(imageRoot, "Каталог изображений");
        return new AppConfiguration(dbUrl, dbUser, dbPassword, materialsRoot, imageRoot, styleGuide);
    }

    private static String requireProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Отсутствует параметр конфигурации: " + key);
        }
        return value.trim();
    }

    private static Path resolvePath(String value) {
        Path path = Paths.get(value);
        if (!path.isAbsolute()) {
            path = Paths.get(System.getProperty("user.dir"), value);
        }
        return path.normalize();
    }

    private static void ensureDirectory(Path path, String label) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        if (!Files.isDirectory(path)) {
            throw new IOException(label + " не является директорией: " + path);
        }
    }

    public String databaseUrl() {
        return databaseUrl;
    }

    public String databaseUser() {
        return databaseUser;
    }

    public String databasePassword() {
        return databasePassword;
    }

    public Path materialsRoot() {
        return materialsRoot;
    }

    public Path imageRoot() {
        return imageRoot;
    }

    public Path styleGuidePath() {
        return styleGuidePath;
    }
}
