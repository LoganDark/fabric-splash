package net.logandark.splash

import net.minecraft.client.MinecraftClient
import org.apache.logging.log4j.LogManager
import java.io.File

object SplashConfig {
	private val LOGGER = LogManager.getFormatterLogger()
	private val configDir =
		File(MinecraftClient.getInstance().runDirectory, "config")
	private val configFile = File(configDir, "splash.cfg")

	const val defaultFg = 0xEF323D
	const val defaultBg = 0x171414

	var fgColor = defaultFg
	var bgColor = defaultBg

	val bgColorArgb get() = bgColor or 0xFF000000.toInt()

	@Suppress("SameParameterValue")
	private fun read(file: File): HashMap<String, String> {
		val map = HashMap<String, String>()

		file.forEachLine {
			val trimmed = it.trim()

			if (trimmed.startsWith('#')) return@forEachLine

			if (!trimmed.contains('=')) {
				if (trimmed.isNotEmpty())
					LOGGER.warn("Skipping malformed config line: %s", trimmed)

				return@forEachLine
			}

			val (key, value) = it.split('=', limit = 2).map(String::trim)

			if (map.containsKey(key))
				LOGGER.warn("Ignoring duplicate declaration: %s", trimmed)
			else
				map[key] = value
		}

		return map
	}

	@Suppress("SameParameterValue")
	private fun apply(file: File, changes: LinkedHashMap<String, String>) {
		val seen = HashSet<String>()
		val sb = StringBuilder()

		// attempt to reuse existing lines
		file.forEachLine {
			val trimmed = it.trim()

			if (trimmed.startsWith('#')) {
				sb.appendln(trimmed)

				return@forEachLine
			}

			if (!trimmed.contains('=')) {
				if (trimmed.isEmpty())
					sb.appendln()

				return@forEachLine
			}

			val (key, value) = it.split('=', limit = 2).map(String::trim)

			if (!seen.add(key))
				return@forEachLine

			sb.appendln(key + "=" + changes.getOrDefault(key, value))
		}

		// add new ones if necessary
		changes.entries.forEach {
			if (seen.add(it.key)) {
				sb.appendln(it.key + "=" + it.value)
			}
		}

		file.writeText(sb.toString())
	}

	private fun parseHex(hex: String): Int? {
		val parsed = try {
			Integer.parseInt(hex, 16)
		} catch (e: NumberFormatException) {
			return null
		}

		return parsed.takeIf { it <= 0xFFFFFF }
	}

	private fun encodeHex(num: Int): String {
		return num.toString(16).padStart(6, '0')
	}

	private fun resetConfig() {
		val sb = StringBuilder()
		sb.appendln("# Edit the hex values below to change the color scheme")
		sb.appendln("# Lines starting with # are ignored, and may even be used to store alternative color schemes")
		sb.appendln()
		sb.appendln("fgColor=" + encodeHex(defaultFg))
		sb.appendln("bgColor=" + encodeHex(defaultBg))
		configFile.writeText(sb.toString())
	}

	private fun makeSureConfigExists() {
		configDir.mkdirs()

		if (configFile.createNewFile()) {
			resetConfig()
		}
	}

	fun save() {
		makeSureConfigExists()

		val changes = linkedMapOf(
			"fgColor" to encodeHex(fgColor),
			"bgColor" to encodeHex(bgColor)
		)

		apply(configFile, changes)
	}

	fun load() {
		makeSureConfigExists()

		val map = read(configFile)

		map.entries.forEach { decl ->
			when (decl.key) {
				"fgColor" -> parseHex(decl.value)?.also { fgColor = it }
				             ?: LOGGER.warn("Invalid hex color code: %s", decl.value)
				"bgColor" -> parseHex(decl.value)?.also { bgColor = it }
				             ?: LOGGER.warn("Invalid hex color code: %s", decl.value)
				else      -> LOGGER.warn("Invalid variable: %s", decl.key)
			}
		}
	}
}
