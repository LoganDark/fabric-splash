package net.logandark.splash

import net.minecraft.client.MinecraftClient
import org.apache.logging.log4j.LogManager
import java.io.File

object SplashConfig {
	private val LOGGER = LogManager.getFormatterLogger()
	private val configDir =
		File(MinecraftClient.getInstance().runDirectory, "config")
	private val configFile = File(configDir, "splash.cfg")

	const val defaultBg = 0x171414
	const val defaultFg = 0xEF323D

	private var config = ""

	var colorBackground = defaultBg
	var colorLogo = defaultFg

	var colorBarBorder = defaultFg
	var colorBarBg = defaultBg
	var colorBarFg = defaultFg

	val colorBackgroundArgb get() = colorBackground or 0xFF000000.toInt()

	@Suppress("SameParameterValue")
	private fun read(config: String): HashMap<String, String> {
		val map = HashMap<String, String>()

		config.lines().forEach {
			val trimmed = it.trim()

			if (trimmed.startsWith('#')) return@forEach

			if (!trimmed.contains('=')) {
				if (trimmed.isNotEmpty())
					LOGGER.warn("Skipping malformed config line: %s", trimmed)

				return@forEach
			}

			val (key, value) = trimmed.split('=', limit = 2).map(String::trim)

			if (map.containsKey(key))
				LOGGER.warn("Ignoring duplicate declaration: %s", trimmed)
			else
				map[key] = value
		}

		return map
	}

	private fun trailingNewlineFix(lines: List<String>): List<String> {
		if (lines.last().isEmpty()) {
			return lines.dropLast(1)
		}

		return lines
	}

	@Suppress("SameParameterValue")
	private fun apply(config: String, changes: LinkedHashMap<String, String>): String {
		val seen = HashSet<String>()
		val sb = StringBuilder()

		// attempt to reuse existing lines
		trailingNewlineFix(config.lines()).forEach {
			val trimmed = it.trim()

			if (trimmed.startsWith('#')) {
				sb.appendln(trimmed)

				return@forEach
			}

			if (!trimmed.contains('=')) {
				if (trimmed.isEmpty())
					sb.appendln()

				return@forEach
			}

			val (key, value) = trimmed.split('=', limit = 2).map(String::trim)

			if (!seen.add(key))
				return@forEach

			sb.appendln(key + "=" + changes.getOrDefault(key, value))
		}

		// add new ones if necessary
		changes.entries.forEach {
			if (seen.add(it.key)) {
				sb.appendln(it.key + "=" + it.value)
			}
		}

		return sb.toString()
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
		sb.appendln("# Lines starting with # are ignored, and may be used to store alternative color schemes")
		sb.appendln()
		sb.appendln("bg=" + encodeHex(defaultBg))
		sb.appendln("logo=" + encodeHex(defaultFg))
		sb.appendln("barBorder=" + encodeHex(defaultFg))
		sb.appendln("barBg=" + encodeHex(defaultBg))
		sb.appendln("barFg=" + encodeHex(defaultFg))
		configFile.writeText(sb.toString())
	}

	private fun makeSureConfigExists() {
		configDir.mkdirs()

		if (configFile.createNewFile()) {
			resetConfig()
			config = configFile.readText()
		}
	}

	fun save() {
		makeSureConfigExists()

		val changes = linkedMapOf(
			"bg" to encodeHex(colorBackground),
			"logo" to encodeHex(colorLogo),
			"barBorder" to encodeHex(colorBarBorder),
			"barBg" to encodeHex(colorBarBg),
			"barFg" to encodeHex(colorBarFg)
		)
		config = apply(config, changes)
		configFile.writeText(config)
	}

	private fun preprocess(config: String): String {
		val sb = StringBuilder()

		trailingNewlineFix(config.lines()).forEach {
			val trimmed = it.trim()

			if (trimmed.startsWith('#') || !trimmed.contains('=')) {
				sb.appendln(it)
				return@forEach
			}

			var (key, value) = trimmed.split('=', limit = 2).map(String::trim)

			// migrate old names
			if (key == "fgColor") {
				key = "logo"
			} else if (key == "bgColor") {
				key = "bg"
			}

			sb.appendln("$key=$value")
		}

		return sb.toString()
	}

	fun load() {
		makeSureConfigExists()

		config = preprocess(configFile.readText())

		val map = read(config)

		fun parseHexOrWarn(something: String): Int? {
			return parseHex(something)
			       ?: LOGGER.warn("Invalid hex color code: %s", something).let { null }
		}

		map.entries.forEach { decl ->
			when (decl.key) {
				"bg"        -> parseHexOrWarn(decl.value)?.also { colorBackground = it }
				"logo"      -> parseHexOrWarn(decl.value)?.also { colorLogo = it }
				"barBorder" -> parseHexOrWarn(decl.value)?.also { colorBarBorder = it }
				"barBg"     -> parseHexOrWarn(decl.value)?.also { colorBarBg = it }
				"barFg"     -> parseHexOrWarn(decl.value)?.also { colorBarFg = it }
				else        -> LOGGER.warn("Unknown variable name: %s", decl.key)
			}
		}
	}
}
