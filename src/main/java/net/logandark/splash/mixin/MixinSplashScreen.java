package net.logandark.splash.mixin;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.logandark.splash.SplashConfig;
import net.minecraft.client.gui.screen.SplashScreen;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SplashScreen.class)
public abstract class MixinSplashScreen {
	@SuppressWarnings("ShadowModifiers")
	@Shadow(remap = false)
	private static int field_25041; // bgArgb

	@SuppressWarnings("ShadowModifiers")
	@Shadow(remap = false)
	private static int field_25042; // bgRgb

	@Unique
	private static int fgRgb;

	@Inject(
		method = "render",
		at = @At("HEAD")
	)
	private void splash_onRender(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		field_25042 = SplashConfig.INSTANCE.getBgColor(); // bgRgb
		field_25041 = field_25042 | 0xFF000000; // bgArgb
		fgRgb = SplashConfig.INSTANCE.getFgColor();
	}

	@Redirect(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;color4f(FFFF)V"
		)
	)
	private void splash_onRender(float ir, float ig, float ib, float alpha) {
		//noinspection deprecation
		RenderSystem.color4f(
			((fgRgb >> 16) & 0xFF) / 255F,
			((fgRgb >> 8) & 0xFF) / 255F,
			(fgRgb & 0xFF) / 255F,
			alpha
		);
	}

	@Inject(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lcom/mojang/blaze3d/systems/RenderSystem;alphaFunc(IF)V",
			shift = At.Shift.BY,
			by = 1
		)
	)
	private void splash_onBlend(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		RenderSystem.blendFuncSeparate(
			GlStateManager.SrcFactor.SRC_ALPHA,
			GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA,
			GlStateManager.SrcFactor.ZERO,
			GlStateManager.DstFactor.ONE
		);
	}

	@Redirect(
		method = "renderProgressBar",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/gui/hud/BackgroundHelper$ColorMixer;getArgb(IIII)I"
		)
	)
	private int splash_onGetArgb(int a, int r, int g, int b) {
		return (a << 24) | fgRgb;
	}
}
