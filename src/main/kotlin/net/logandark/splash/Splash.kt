package net.logandark.splash

import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

@Suppress("unused")
object Splash : ClientModInitializer {
	private val LOGGER: Logger = LogManager.getLogger()

	override fun onInitializeClient() {
		LOGGER.info("Hello Fabric world!")

		SplashConfig.load()
		SplashConfig.save()
	}
}
