package com.gabbymaybelle.dracora;

import com.gabbymaybelle.dracora.platform.Services;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Properties;

/**
 * The settings, in {@code config/dracora.properties}. A missing file is written out with the
 * defaults and a comment per setting; a value that can't be read keeps the default, with a warning
 * in the log. Read once as the game starts, before any world is made.
 */
public final class Config {
	public static final String FILE_NAME = Dracora.MOD_ID + ".properties";
	public static final int NEST_SPACING_DEFAULT = 100;
	public static final int NEST_SPACING_MAX = 1000;

	/**
	 * How far apart nests are, in percent of the usual distance: 100 is as designed, 200 puts them
	 * twice as far apart (about a quarter as many), 50 half as far (about four times as many), and
	 * 0 turns nests off. Applies to Overworld and End nests alike, in chunks generated from then on.
	 */
	public static int nestSpacing = NEST_SPACING_DEFAULT;

	private static final String NEST_SPACING_COMMENT = """
			How far apart dragon nests are, in percent of the usual distance (Overworld and End).
			# 100 is as designed; 200 puts them twice as far apart (about a quarter as many);
			# 50 puts them half as far apart (about four times as many); 0 means no nests.
			# Only land generated after the change is affected. From 0 to 1000.""";

	private Config() {
	}

	/** The usual spacing at a setting in percent, in chunks; never below 2, so there is room to spread. */
	public static int spacingAt(int usual, int percent) {
		return Math.max(2, Math.round(usual * percent / 100.0F));
	}

	/** The usual separation at a setting, always at least one chunk less than the spacing, as vanilla requires. */
	public static int separationAt(int usualSpacing, int usualSeparation, int percent) {
		return Math.min(Math.round(usualSeparation * percent / 100.0F), spacingAt(usualSpacing, percent) - 1);
	}

	public static Path path() {
		return Services.PLATFORM.configDir().resolve(FILE_NAME);
	}

	public static void load() {
		Path file = path();
		Properties props = new Properties();
		if (Files.exists(file)) {
			try (Reader in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
				props.load(in);
			} catch (IOException e) {
				Dracora.LOGGER.warn("Couldn't read {}; using the defaults", file, e);
			}
		}
		nestSpacing = readInt(props, "nestSpacing", NEST_SPACING_DEFAULT, 0, NEST_SPACING_MAX);
		if (!props.containsKey("nestSpacing")) {
			save(file);
		}
	}

	private static int readInt(Properties props, String key, int fallback, int min, int max) {
		String raw = props.getProperty(key);
		if (raw == null) {
			return fallback;
		}
		try {
			int value = Integer.parseInt(raw.trim());
			if (value < min || value > max) {
				Dracora.LOGGER.warn("{} must be between {} and {} (got {}); using {}", key, min, max, value, fallback);
				return fallback;
			}
			return value;
		} catch (NumberFormatException e) {
			Dracora.LOGGER.warn("{} should be a whole number (got \"{}\"); using {}", key, raw, fallback);
			return fallback;
		}
	}

	/** Writes the settings through a temp file, so a crash mid-write can't leave it half done. */
	private static void save(Path file) {
		try {
			Files.createDirectories(file.getParent());
			Path temp = file.resolveSibling(FILE_NAME + ".tmp");
			try (Writer out = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
				out.write("# Dracora settings. Changes apply when the game starts.\n");
				out.write("\n# " + NEST_SPACING_COMMENT + "\n");
				out.write("nestSpacing=" + nestSpacing + "\n");
			}
			Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			Dracora.LOGGER.warn("Couldn't write {}", file, e);
		}
	}
}
