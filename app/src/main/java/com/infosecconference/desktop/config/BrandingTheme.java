package com.infosecconference.desktop.config;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Reads branding information from {@code style/branding.properties} that was prepared using the style guide.
 */
public final class BrandingTheme {
    private static final String RESOURCE = "style/branding.properties";

    private final Color primaryColor;
    private final Color secondaryColor;
    private final Color accentColor;
    private final Color surfaceColor;
    private final Font baseFont;
    private final Font titleFont;

    private BrandingTheme(Color primaryColor,
                          Color secondaryColor,
                          Color accentColor,
                          Color surfaceColor,
                          Font baseFont,
                          Font titleFont) {
        this.primaryColor = primaryColor;
        this.secondaryColor = secondaryColor;
        this.accentColor = accentColor;
        this.surfaceColor = surfaceColor;
        this.baseFont = baseFont;
        this.titleFont = titleFont;
    }

    public static BrandingTheme load() throws IOException {
        Properties properties = new Properties();
        try (InputStream stream = BrandingTheme.class.getClassLoader().getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IOException("Не найден файл брендбука: " + RESOURCE);
            }
            properties.load(stream);
        }

        Color primary = parseColor(properties.getProperty("color.primary", "#1C355E"));
        Color secondary = parseColor(properties.getProperty("color.secondary", "#14213D"));
        Color accent = parseColor(properties.getProperty("color.accent", "#FCA311"));
        Color surface = parseColor(properties.getProperty("color.surface", "#F4F6FA"));

        Font baseFont = new Font(properties.getProperty("font.base.name", UIManager.getFont("Label.font").getName()),
                Font.PLAIN,
                Integer.parseInt(properties.getProperty("font.base.size", "14")));
        Font titleFont = baseFont.deriveFont(Font.BOLD,
                Float.parseFloat(properties.getProperty("font.title.size", "24")));

        return new BrandingTheme(primary, secondary, accent, surface, baseFont, titleFont);
    }

    private static Color parseColor(String value) {
        return Color.decode(value.trim());
    }

    public Color primaryColor() {
        return primaryColor;
    }

    public Color secondaryColor() {
        return secondaryColor;
    }

    public Color accentColor() {
        return accentColor;
    }

    public Color surfaceColor() {
        return surfaceColor;
    }

    public Font baseFont() {
        return baseFont;
    }

    public Font titleFont() {
        return titleFont;
    }
}
