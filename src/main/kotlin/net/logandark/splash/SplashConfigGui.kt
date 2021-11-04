package net.logandark.splash

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.MathHelper
import org.lwjgl.glfw.GLFW.GLFW_KEY_GRAVE_ACCENT
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_GREATER
import org.lwjgl.opengl.GL11.GL_LINEAR
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import org.lwjgl.opengl.GL14
import kotlin.math.min

class SplashConfigGui(
	private val parent: Screen
) : Screen(TranslatableText("splash.configuration.title")) {
	private lateinit var fieldBackground: HexTextFieldWidget
	private lateinit var fieldLogo: HexTextFieldWidget
	private lateinit var fieldBarBorder: HexTextFieldWidget
	private lateinit var fieldBarBg: HexTextFieldWidget
	private lateinit var fieldBarFg: HexTextFieldWidget

	private val colorBackgroundStart = SplashConfig.colorBackground
	private val colorLogoStart = SplashConfig.colorLogo
	private val colorBarBorderStart = SplashConfig.colorBarBorder
	private val colorBarBgStart = SplashConfig.colorBarBg
	private val colorBarFgStart = SplashConfig.colorBarFg

	private var stringLogo: String? = null
	private var stringBackground: String? = null
	private var stringBarBorder: String? = null
	private var stringBarBg: String? = null
	private var stringBarFg: String? = null

	private var applying = false

	private var buttonsShown = true

	override fun init() {
		super.init()

		val width = client!!.window.scaledWidth
		val halfWidth = width / 2
		val height = client!!.window.scaledHeight

		val padding = 8
		val halfPadding = padding / 2
		val fieldWidth = 44
		val buttonWidth = 50

		fieldLogo = HexTextFieldWidget(
			textRenderer, width - padding - fieldWidth, padding, fieldWidth, 20,
			TranslatableText("splash.configuration.fgBox")
		)

		if (stringLogo != null)
			fieldLogo.text = stringLogo
		else
			fieldLogo.setValue(SplashConfig.colorLogo)

		fieldBackground = HexTextFieldWidget(
			textRenderer, padding, padding, fieldWidth, 20,
			TranslatableText("splash.configuration.bgBox")
		)

		if (stringBackground != null)
			fieldBackground.text = stringBackground
		else
			fieldBackground.setValue(SplashConfig.colorBackground)

		fieldBarBorder = HexTextFieldWidget(
			textRenderer, padding, padding * 5 + 80, fieldWidth, 20,
			TranslatableText("splash.configuration.barBorderBox")
		)

		if (stringBarBorder != null)
			fieldBarBorder.text = stringBarBorder
		else
			fieldBarBorder.setValue(SplashConfig.colorBarBorder)

		fieldBarBg = HexTextFieldWidget(
			textRenderer, padding, padding * 6 + 100, fieldWidth, 20,
			TranslatableText("splash.configuration.barBgBox")
		)

		if (stringBarBg != null)
			fieldBarBg.text = stringBarBg
		else
			fieldBarBg.setValue(SplashConfig.colorBarBg)

		fieldBarFg = HexTextFieldWidget(
			textRenderer, padding, padding * 7 + 120, fieldWidth, 20,
			TranslatableText("splash.configuration.barFgBox")
		)

		if (stringBarFg != null)
			fieldBarFg.text = stringBarFg
		else
			fieldBarFg.setValue(SplashConfig.colorBarFg)

		addDrawableChild(fieldLogo)
		addDrawableChild(fieldBackground)
		addDrawableChild(fieldBarBorder)
		addDrawableChild(fieldBarBg)
		addDrawableChild(fieldBarFg)

		val presets = listOf<Pair<Text, Pair<Int, Int>>>(
			TranslatableText("splash.configuration.preset.default") to (SplashConfig.defaultFg to SplashConfig.defaultBg),
			TranslatableText("splash.configuration.preset.vanilla") to (0xFFFFFF to 0xEF323D)
		)

		for (i in 0 until presets.size) {
			val (key, values) = presets[i]

			addDrawableChild(ButtonWidget(
				padding, padding + (20 + padding) * (i + 1), fieldWidth, 20, key,
				ButtonWidget.PressAction {
					fieldLogo.setValue(values.first)
					fieldBackground.setValue(values.second)
					fieldBarBorder.setValue(values.first)
					fieldBarBg.setValue(values.second)
					fieldBarFg.setValue(values.first)
				}
			))
		}

		addDrawableChild(ButtonWidget(
			halfWidth - halfPadding - buttonWidth, height - 20 - padding, buttonWidth, 20,
			TranslatableText("splash.configuration.cancel"),
			ButtonWidget.PressAction {
				onClose()
			}
		))

		addDrawableChild(ButtonWidget(
			halfWidth + halfPadding, height - 20 - padding, buttonWidth, 20,
			TranslatableText("splash.configuration.save"),
			ButtonWidget.PressAction {
				applying = true
				onClose()
			}
		))
	}

	override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
		stringLogo = fieldLogo.text
		stringBackground = fieldBackground.text

		val valueBackground = fieldBackground.getValue()
		val valueLogo = fieldLogo.getValue()
		val valueBarBorder = fieldBarBorder.getValue()
		val valueBarBg = fieldBarBg.getValue()
		val valueBarFg = fieldBarFg.getValue()

		if (valueBackground != null)
			SplashConfig.colorBackground = valueBackground
		if (valueLogo != null)
			SplashConfig.colorLogo = valueLogo
		if (valueBarBorder != null)
			SplashConfig.colorBarBorder = valueBarBorder
		if (valueBarBg != null)
			SplashConfig.colorBarBg = valueBarBg
		if (valueBarFg != null)
			SplashConfig.colorBarFg = valueBarFg

		renderSplash(matrices)

		if (buttonsShown)
			super.render(matrices, mouseX, mouseY, delta)
	}

	@Suppress("DEPRECATION")
	private fun renderSplash(matrices: MatrixStack) {
		val width = client!!.window.scaledWidth
		val height = client!!.window.scaledHeight

		// background color
		fill(matrices, 0, 0, width, height, SplashConfig.colorBackgroundArgb)

		val xCenter = width / 2
		val yCenter = height / 2
		val sizeX = min(width * 3 / 4, height)
		val hsizeX = sizeX / 2
		val sizeY = hsizeX / 2
		val hsizeY = sizeY / 2

		val fg = SplashConfig.colorLogo
		val red = (fg shr 16 and 0xFF).toFloat() / 255
		val green = (fg shr 8 and 0xFF).toFloat() / 255
		val blue = (fg and 0xFF).toFloat() / 255

		Splash.bindLogoTexture()
		RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
		RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

		val prevWrapS = GL11.glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S)
		RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)

		RenderSystem.enableBlend()
		GL14.glBlendColor(red, green, blue, 1F)
		RenderSystem.blendFuncSeparate(
			GlStateManager.SrcFactor.CONSTANT_COLOR,
			GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
			GlStateManager.SrcFactor.ZERO,
			GlStateManager.DstFactor.ONE
		)
		//RenderSystem.alphaFunc(GL_GREATER, 0.0f)
		RenderSystem.setShaderColor(1F, 1F, 1F, 1F)
		drawTexture(matrices, xCenter - hsizeX, yCenter - hsizeY, hsizeX, sizeY, -0.0625f, 0.0f, 120, 60, 120, 120)
		drawTexture(matrices, xCenter, yCenter - hsizeY, hsizeX, sizeY, 0.0625f, 60.0f, 120, 60, 120, 120)
		RenderSystem.defaultBlendFunc()
		//RenderSystem.defaultAlphaFunc()
		RenderSystem.disableBlend()

		RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, prevWrapS)

		val yBarCenter = (height.toDouble() * 0.8325).toInt()
		val progress = 0.9525
		renderProgressBar(matrices, xCenter - hsizeX, yBarCenter - 5, xCenter + hsizeX, yBarCenter + 5, 1.0, progress)
	}

	@Suppress("SameParameterValue")
	private fun renderProgressBar(
		matrices: MatrixStack,
		x1: Int,
		y1: Int,
		x2: Int,
		y2: Int,
		alpha: Double,
		progress: Double
	) {
		val progressPixels = MathHelper.ceil((x2 - x1 - 2) * progress)

		val alphaBits = (alpha * 255.999).toInt() shl 24
		val colorBarBorder = alphaBits or SplashConfig.colorBarBorder
		val colorBarBg = alphaBits or SplashConfig.colorBarBg
		val colorBarFg = alphaBits or SplashConfig.colorBarFg

		fill(matrices, x1 + 1, y1, x2 - 1, y1 + 1, colorBarBorder)
		fill(matrices, x1 + 1, y2, x2 - 1, y2 - 1, colorBarBorder)
		fill(matrices, x1, y1, x1 + 1, y2, colorBarBorder)
		fill(matrices, x2, y1, x2 - 1, y2, colorBarBorder)

		fill(matrices, x1 + 1, y1 + 1, x2 - 1, y2 - 1, colorBarBg)
		fill(matrices, x1 + 2, y1 + 2, x1 + progressPixels, y2 - 2, colorBarFg)
	}

	override fun onClose() {
		if (applying) {
			SplashConfig.save()
		} else {
			SplashConfig.colorBackground = colorBackgroundStart
			SplashConfig.colorLogo = colorLogoStart
			SplashConfig.colorBarBorder = colorBarBorderStart
			SplashConfig.colorBarBg = colorBarBgStart
			SplashConfig.colorBarFg = colorBarFgStart
		}

		client!!.setScreen(parent)
	}

	override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
		return if (keyCode == GLFW_KEY_GRAVE_ACCENT) {
			buttonsShown = !buttonsShown
			true
		} else {
			super.keyPressed(keyCode, scanCode, modifiers)
		}
	}
}
