package com.gabbymaybelle.dracora.client;

import com.gabbymaybelle.dracora.Dracora;
import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import net.minecraft.client.renderer.texture.ReloadableTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ARGB;

/**
 * The Ender Dragon's own texture, read from the game (or whatever resource pack replaces it) and
 * turned into a bright grey copy that a dragon's colour can tint. Nothing of the vanilla picture
 * ships with the mod: the copy is made here each time resources load, so a pack that repaints the
 * Ender Dragon repaints Dracora's dragons too.
 *
 * <p>The vanilla texture is nearly black, and a tint only darkens, so the grey is stretched: the
 * darkest scale becomes a mid grey and the brightest becomes white.
 */
public final class TintableDragonTexture extends ReloadableTexture {
	/** The id dragons are drawn with. No file sits behind it; this texture is registered under it. */
	public static final Identifier ID = Dracora.id("textures/entity/dragon/tintable_ender_dragon.png");
	public static final Identifier VANILLA = Identifier.withDefaultNamespace("textures/entity/enderdragon/dragon.png");
	/** The grey the darkest pixel becomes, out of 255. */
	private static final int FLOOR = 96;
	/** Below 1 this lifts the middle greys, so the scales' shading stays readable under a tint. */
	private static final double GAMMA = 0.75;

	public TintableDragonTexture() {
		super(ID);
	}

	@Override
	public TextureContents loadContents(ResourceManager resourceManager) throws IOException {
		TextureContents vanilla = TextureContents.load(resourceManager, VANILLA);
		brighten(vanilla.image());
		return vanilla;
	}

	/** Greys and stretches every pixel that isn't see-through, in place. */
	static void brighten(NativeImage image) {
		int width = image.getWidth();
		int height = image.getHeight();
		int[] counts = new int[256];
		int opaque = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = image.getPixel(x, y);
				if (ARGB.alpha(pixel) != 0) {
					counts[grey(pixel)]++;
					opaque++;
				}
			}
		}
		if (opaque == 0) {
			return;
		}
		// The range the picture really uses, ignoring the odd stray pixel at either end.
		int low = percentile(counts, opaque, 0.01);
		int high = Math.max(low + 1, percentile(counts, opaque, 0.99));
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int pixel = image.getPixel(x, y);
				int alpha = ARGB.alpha(pixel);
				if (alpha != 0) {
					int value = stretch(grey(pixel), low, high);
					image.setPixel(x, y, ARGB.color(alpha, value, value, value));
				}
			}
		}
	}

	/** How bright a pixel looks, 0 to 255. */
	static int grey(int pixel) {
		return (ARGB.red(pixel) * 30 + ARGB.green(pixel) * 59 + ARGB.blue(pixel) * 11) / 100;
	}

	/** Maps a grey from the picture's own range onto {@link #FLOOR}..255. */
	static int stretch(int grey, int low, int high) {
		double share = Math.clamp((grey - low) / (double) (high - low), 0.0, 1.0);
		return (int) Math.round(FLOOR + Math.pow(share, GAMMA) * (255 - FLOOR));
	}

	private static int percentile(int[] counts, int total, double share) {
		long target = Math.round(total * share);
		long seen = 0;
		for (int value = 0; value < counts.length; value++) {
			seen += counts[value];
			if (seen > target) {
				return value;
			}
		}
		return counts.length - 1;
	}
}
