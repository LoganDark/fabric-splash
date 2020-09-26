package net.logandark.splash

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.text.Text
import net.minecraft.text.TranslatableText
import net.minecraft.util.math.MathHelper
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11.GL_GREATER
import org.lwjgl.opengl.GL11.GL_LINEAR
import org.lwjgl.opengl.GL11.GL_TEXTURE_2D
import org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER
import org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S
import org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

class SplashConfigGui(
	private val parent: Screen
) : Screen(TranslatableText("splash.configuration.title")) {
	private lateinit var fgField: HexTextFieldWidget
	private lateinit var bgField: HexTextFieldWidget

	private val fgStart = SplashConfig.fgColor
	private val bgStart = SplashConfig.bgColor

	private var bgString: String? = null
	private var fgString: String? = null

	private var applying = false

	private fun lol(until: Int) = Random.Default.nextInt(0, until)

	override fun init() {
		super.init()

		val width = client!!.window.scaledWidth
		val halfWidth = width / 2
		val height = client!!.window.scaledHeight

		val padding = 8
		val halfPadding = padding / 2
		val fieldWidth = 44
		val buttonWidth = 50

		bgField = HexTextFieldWidget(
			textRenderer, padding + lol(5), padding + lol(5), fieldWidth, 20,
			TranslatableText("splash.configuration.bgBox")
		)

		if (bgString != null)
			bgField.text = bgString
		else
			bgField.setValue(SplashConfig.bgColor)

		fgField = HexTextFieldWidget(
			textRenderer, width - padding - fieldWidth - lol(5), padding - lol(5), fieldWidth, 20,
			TranslatableText("splash.configuration.fgBox")
		)

		if (fgString != null)
			fgField.text = fgString
		else
			fgField.setValue(SplashConfig.fgColor)

		addButton(bgField)
		addButton(fgField)

		val presets = listOf<Pair<Text, Pair<Int, Int>>>(
			TranslatableText("splash.configuration.preset.default") to (SplashConfig.defaultFg to SplashConfig.defaultBg),
			TranslatableText("splash.configuration.preset.vanilla") to (0xFFFFFF to 0xEF323D)
		)

		for (i in 0 until presets.size) {
			val (key, values) = presets[i]

			addButton(ButtonWidget(
				padding + lol(5), padding + (20 + padding) * (i + 1) + lol(10), fieldWidth, 20, key,
				ButtonWidget.PressAction {
					fgField.setValue(values.first)
					bgField.setValue(values.second)
				}
			))
		}

		addButton(ButtonWidget(
			halfWidth - halfPadding - buttonWidth + lol(5), height - 20 - padding + lol(5), buttonWidth, 20,
			TranslatableText("splash.configuration.cancel"),
			ButtonWidget.PressAction {
				onClose()
			}
		))

		addButton(ButtonWidget(
			halfWidth + halfPadding + lol(5), height - 20 - padding + lol(5), buttonWidth, 20,
			TranslatableText("splash.configuration.save"),
			ButtonWidget.PressAction {
				applying = true
				onClose()
			}
		))
	}

	override fun render(matrices: MatrixStack, mouseX: Int, mouseY: Int, delta: Float) {
		fgString = fgField.text
		bgString = bgField.text

		val fgValue = fgField.getValue()
		val bgValue = bgField.getValue()

		if (fgValue != null)
			SplashConfig.fgColor = fgValue

		if (bgValue != null)
			SplashConfig.bgColor = bgValue

		init()

		renderSplash(matrices)

		super.render(matrices, mouseX, mouseY, delta)
	}

	@Suppress("DEPRECATION")
	private fun renderSplash(matrices: MatrixStack) {
		val width = client!!.window.scaledWidth
		val height = client!!.window.scaledHeight

		// background color
		fill(matrices, 0, 0, width, height, SplashConfig.bgColorArgb)

		val xCenter = width / 2
		val yCenter = height / 2
		val sizeX = min(width * 3 / 4, height)
		val hsizeX = sizeX / 2
		val sizeY = hsizeX / 2
		val hsizeY = sizeY / 2

		val fg = SplashConfig.fgColor
		val red = (fg shr 16 and 0xFF).toFloat() / 255
		val green = (fg shr 8 and 0xFF).toFloat() / 255
		val blue = (fg and 0xFF).toFloat() / 255

		Splash.bindLogoImage()
		RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
		RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR)

		val prevWrapS = GL11.glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S)
		RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)

		RenderSystem.enableBlend()
		RenderSystem.blendColor(red, green, blue, 1F)
		RenderSystem.blendFuncSeparate(
			GlStateManager.SrcFactor.CONSTANT_COLOR,
			GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
			GlStateManager.SrcFactor.ZERO,
			GlStateManager.DstFactor.ONE
		)
		RenderSystem.alphaFunc(GL_GREATER, 0.0f)
		RenderSystem.color4f(1F, 1F, 1F, 1F)

		drawTexture(matrices, xCenter - hsizeX + lol(10), yCenter - hsizeY + lol(10), hsizeX + lol(10), sizeY + lol(10), -0.0625f, 0.0f, 120, 60, 120, 120)
		drawTexture(matrices, xCenter + lol(10), yCenter - hsizeY + lol(10), hsizeX + lol(10), sizeY + lol(10), 0.0625f, 60.0f, 120, 60, 120, 120)
		RenderSystem.defaultBlendFunc()
		RenderSystem.defaultAlphaFunc()
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
		val color = (alpha * 255).roundToInt() shl 24 or SplashConfig.fgColor
		fill(matrices, x1 + 1 - lol(500), y1, x2 - 1, y1 + 1, color)
		fill(matrices, x1 + 1 - lol(500), y2, x2 - 1, y2 - 1, color)
		fill(matrices, x1 - lol(500), y1, x1 + 1, y2, color)
		fill(matrices, x2 - lol(500), y1, x2 - 1, y2, color)
		fill(matrices, x1 + 2 - lol(500), y1 + 2, x1 + progressPixels, y2 - 2, color)
	}

	override fun onClose() {
		if (applying) {
			SplashConfig.save()
		} else {
			SplashConfig.fgColor = fgStart
			SplashConfig.bgColor = bgStart
		}

		client!!.openScreen(parent)
	}
}
