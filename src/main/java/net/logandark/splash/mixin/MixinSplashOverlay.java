package net.logandark.splash.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.logandark.splash.Splash;
import net.logandark.splash.SplashConfig;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;

import static org.lwjgl.opengl.GL11.GL_LINEAR;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;

@Mixin(SplashOverlay.class)
public abstract class MixinSplashOverlay {
	@Final
	@Shadow
	private static IntSupplier BRAND_ARGB;

	@Unique
	private static int colorBgRgb;
	@Unique
	private static int colorLogoRgb;
	@Unique
	private static int colorBarBorderRgb;
	@Unique
	private static int colorBarBgRgb;
	@Unique
	private static int colorBarFgRgb;
	@Unique
	private static int alpha;

	@Unique
	private static int prevWrapS;

	@Inject(
		method = "render",
		at = @At("HEAD")
	)
	private void splash_onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		colorBgRgb = SplashConfig.INSTANCE.getColorBackground();
		colorLogoRgb = SplashConfig.INSTANCE.getColorLogo();
		colorBarBorderRgb = SplashConfig.INSTANCE.getColorBarBorder();
		colorBarBgRgb = SplashConfig.INSTANCE.getColorBarBg();
		colorBarFgRgb = SplashConfig.INSTANCE.getColorBarFg();
	}

	@Redirect(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Ljava/util/function/IntSupplier;getAsInt()I"
		)
	)
	private int splash_onGetAsInt(IntSupplier instance) {
		if (instance == BRAND_ARGB) {
			return colorBgRgb;
		}

		return instance.getAsInt();
	}

	@Redirect(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/util/Identifier;)V"
		)
	)
	private void splash_bindTexture(int i, Identifier identifier) {
		// switch to premultiplied logo
		Splash.INSTANCE.bindLogoTexture();

		// bilinear scaling
		RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

		prevWrapS = GL11.glGetTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S);

		// fix lines on edges
		RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
	}

	@Redirect(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderColor(FFFF)V"
		)
	)
	private void splash_onRender(float ir, float ig, float ib, float alpha) {
		GL14.glBlendColor(
			((colorLogoRgb >> 16) & 0xFF) / 255F * alpha,
			((colorLogoRgb >> 8) & 0xFF) / 255F * alpha,
			(colorLogoRgb & 0xFF) / 255F * alpha,
			alpha
		);

		RenderSystem.blendFuncSeparate(
			GlStateManager.SrcFactor.CONSTANT_COLOR,
			GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
			GlStateManager.SrcFactor.ZERO,
			GlStateManager.DstFactor.ONE
		);

		RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
	}

	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;disableBlend()V",
			shift = At.Shift.AFTER
		)
	)
	private void splash_afterRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		// looks like this is not restored automatically by MC. prevent side effects
		RenderSystem.texParameter(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, prevWrapS);
	}

	/**
	 * Used to draw the background color underneath the progress bar
	 */
	@Inject(
		method = "renderProgressBar",
		at = @At("HEAD")
	)
	private void splash_onBeforeDrawBarBorder(MatrixStack matrices, int x1, int y1, int x2, int y2, float a, CallbackInfo ci) {
		alpha = Math.round(a * 255.0F) << 24;
		DrawableHelper.fill(matrices, x1 + 1, y1 + 1, x2 - 1, y2 - 1, alpha | colorBarBgRgb);
	}

	/**
	 * Used to change the color of the progress bar border
	 */
	@Redirect(
		method = "renderProgressBar",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/hud/BackgroundHelper$ColorMixer;getArgb(IIII)I"
		)
	)
	private int splash_onGetBarBorderArgb(int a, int r, int g, int b) {
		return alpha | colorBarBorderRgb;
	}

	/**
	 * Used to change the color of the progress bar itself
	 */
	@ModifyArg(
		method = "renderProgressBar",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/screen/SplashOverlay;fill(Lnet/minecraft/client/util/math/MatrixStack;IIIII)V",
			ordinal = 4
		),
		index = 5
	)
	private int splash_getBarFgArgb(int _unused) {
		return alpha | colorBarFgRgb;
	}
}
