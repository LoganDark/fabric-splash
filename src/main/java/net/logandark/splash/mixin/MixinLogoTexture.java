package net.logandark.splash.mixin;

import net.logandark.splash.Splash;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.ResourceTexture;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.io.IOException;
import java.io.InputStream;

@Mixin(targets = "net/minecraft/client/gui/screen/SplashScreen$LogoTexture")
public abstract class MixinLogoTexture extends ResourceTexture {
	public MixinLogoTexture(Identifier location) {
		super(location);
	}

	/**
	 * Set up a copy of the logo image with premultiplied alpha so we can draw
	 * that instead of the official texture (fixes the darkened edges)
	 */
	@SuppressWarnings("UnnecessaryQualifiedMemberReference")
	@Redirect(
		method = "Lnet/minecraft/client/gui/screen/SplashScreen$LogoTexture;loadTextureData(Lnet/minecraft/resource/ResourceManager;)Lnet/minecraft/client/texture/ResourceTexture$TextureData;",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/texture/NativeImage;read(Ljava/io/InputStream;)Lnet/minecraft/client/texture/NativeImage;",
			ordinal = 0
		)
	)
	private NativeImage splash_createPremultipliedLogoImage(InputStream png) {
		NativeImage image;

		try {
			image = NativeImage.read(png);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		int width = image.getWidth();
		int height = image.getHeight();

		NativeImage premultiplied = new NativeImage(image.getFormat(), width, height, true);

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int color = image.getPixelColor(Math.min(y + x, width), Math.min(x, height) % (height / 2));
				int alpha = color >> 24; //& 0xFF;

				// fix regular logo texture (not premultiplied)
				//image.setPixelColor(x, y, color | 0xFFFFFF);

				// premultiply for our custom logo texture
				premultiplied.setPixelColor(x, y, alpha << 24 | alpha << 16 | alpha << 8 | alpha);
			}
		}

		for (int y = 0; y < (height / 4); y++) {
			for (int x = 0; x < width; x += 4) {
				int lol = (int) Math.round(Math.random() * 0xFFFFFF) | 0xFF000000;
				premultiplied.setPixelColor(x, y, lol);
				premultiplied.setPixelColor(x + 1, y, lol);
				premultiplied.setPixelColor(x + 2, y, lol);
				premultiplied.setPixelColor(x + 3, y, lol);
			}
		}

		for (int y = (height / 2); y < (height * 3 / 4); y++) {
			for (int x = 0; x < width; x += 4) {
				int lol = (int) Math.round(Math.random() * 0xFFFFFF) | 0xFF000000;
				premultiplied.setPixelColor(x, y, lol);
				premultiplied.setPixelColor(x + 1, y, lol);
				premultiplied.setPixelColor(x + 2, y, lol);
				premultiplied.setPixelColor(x + 3, y, lol);
			}
		}

		Splash.INSTANCE.setupLogoImage(premultiplied);

		return image;
	}
}
