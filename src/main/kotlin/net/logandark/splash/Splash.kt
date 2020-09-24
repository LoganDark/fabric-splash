package net.logandark.splash

import net.fabricmc.api.ModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Suppress("unused")
object Splash : ModInitializer {
	private val LOGGER: Logger = LogManager.getLogger()

	override fun onInitialize() {
		LOGGER.info("Hello Fabric world!")

		SplashConfig.load()
		SplashConfig.save()
	}
}
