package net.logandark.splash

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.widget.TextFieldWidget
import net.minecraft.text.Text

class HexTextFieldWidget(textRenderer: TextRenderer?, x: Int, y: Int, width: Int, height: Int, title: Text)
	: TextFieldWidget(textRenderer, x, y, width, height, title) {
	init {
		setMaxLength(6)
	}

	override fun write(string: String) {
		super.write(string.filter { "0123456789abcdef".contains(it, true) }.toLowerCase())
	}

	fun setValue(value: Int) {
		text = value.toString(16).padStart(6, '0')
	}

	fun getValue(): Int? {
		if (text.length < 6)
			return null

		return try {
			Integer.valueOf(text, 16)
		} catch (e: NumberFormatException) {
			null
		}
	}
}
