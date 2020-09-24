package net.logandark.splash

import io.github.prospector.modmenu.api.ConfigScreenFactory
import io.github.prospector.modmenu.api.ModMenuApi

object SplashModMenu : ModMenuApi {
	override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
		ConfigScreenFactory(::SplashConfigGui)
}
