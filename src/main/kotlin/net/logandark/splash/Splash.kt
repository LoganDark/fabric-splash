package net.logandark.splash

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.NativeImageBackedTexture
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Suppress("unused")
object Splash : ClientModInitializer {
	private val LOGGER: Logger = LogManager.getLogger()

	private var logoTexture: NativeImageBackedTexture? = null

	override fun onInitializeClient() {
		LOGGER.info("Hello Fabric world!")

		SplashConfig.load()
		SplashConfig.save()
	}

	/**
	 * @param image The image to upload as the new logo image
	 */
	fun setupLogoTexture(image: NativeImage) {
		logoTexture = NativeImageBackedTexture(image)
	}

	/**
	 * Binds the premultiplied logo texture as set by [setupLogoTexture]. Our
	 * logo texture is separate from vanilla's since it uses premultiplied alpha
	 * to assist with blending, and if any mods draw the logo that's something
	 * they may not expect
	 *
	 * @throws NullPointerException If [setupLogoTexture] has not been called
	 */
	fun bindLogoTexture() {
		RenderSystem._setShaderTexture(0, logoTexture!!.glId)
	}
}
