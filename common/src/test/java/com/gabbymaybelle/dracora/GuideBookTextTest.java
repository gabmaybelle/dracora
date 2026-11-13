package com.gabbymaybelle.dracora;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Every page the nests' guide book asks for has words in the English language file, and none is too long for a book page. */
class GuideBookTextTest {
	private static final int PAGES = 9;
	/** Roughly what fits on one book page (14 lines of about 19 characters), with some room. */
	private static final int PAGE_LIMIT = 270;

	@Test
	void everyPageHasWordsThatFit() throws Exception {
		String lang;
		try (InputStream in = getClass().getResourceAsStream("/assets/dracora/lang/en_us.json")) {
			assertNotNull(in, "the English language file should be on the classpath");
			lang = new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
		for (int page = 1; page <= PAGES; page++) {
			// Each entry is one line of the file: "key": "text", with the text's line breaks written as \n.
			Matcher entry = Pattern.compile("\"book\\.dracora\\.guide\\.page" + page + "\": \"(.*)\",?\\s*$", Pattern.MULTILINE).matcher(lang);
			assertTrue(entry.find(), "page " + page + " should have words");
			String text = entry.group(1).replace("\\n", "\n");
			assertTrue(!text.isBlank() && text.length() <= PAGE_LIMIT, "page " + page + " should fit on a book page (" + text.length() + " characters)");
		}
		assertTrue(!lang.contains("book.dracora.guide.page" + (PAGES + 1)), "no page beyond " + PAGES + " should be left in the file");
	}
}
