package net.logandark.splash

import com.mojang.blaze3d.platform.GlStateManager
import net.fabricmc.api.ClientModInitializer
import net.minecraft.client.texture.NativeImage
import net.minecraft.client.texture.TextureUtil
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Suppress("unused")
object Splash : ClientModInitializer {
	private val LOGGER: Logger = LogManager.getLogger()

	private var logoGlId: Int? = null

	override fun onInitializeClient() {
		LOGGER.info("Hello Fabric world!")

		SplashConfig.load()
		SplashConfig.save()
	}

	/**
	 * @param image The image to upload as the new logo image
	 */
	fun setupLogoImage(image: NativeImage) {
		val glId = TextureUtil.generateId()

		TextureUtil.allocate(glId, 0, image.width, image.height)
		image.upload(0, 0, 0, false)

		logoGlId = glId
	}

	/**
	 * Binds the premultiplied logo texture as set by [setupLogoImage]. Our logo
	 * texture is separate from vanilla's since it uses premultiplied alpha to
	 * assist with blending, and if any mods draw the logo that's something they
	 * may not expect
	 *
	 * @throws NullPointerException If [setupLogoImage] has not been called
	 */
	fun bindLogoImage() {
		GlStateManager.bindTexture(logoGlId!!)
	}
}
