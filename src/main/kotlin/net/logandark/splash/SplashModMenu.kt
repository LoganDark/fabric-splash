package net.logandark.splash

import com.terraformersmc.modmenu.api.ConfigScreenFactory
import com.terraformersmc.modmenu.api.ModMenuApi

object SplashModMenu : ModMenuApi {
	override fun getModConfigScreenFactory(): ConfigScreenFactory<*> =
		ConfigScreenFactory(::SplashConfigGui)
}
