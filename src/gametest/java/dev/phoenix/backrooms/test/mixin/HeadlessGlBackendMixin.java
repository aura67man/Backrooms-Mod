package dev.phoenix.backrooms.test.mixin;

import com.mojang.renderpearl.backend.opengl.GlBackend;
import org.lwjgl.sdl.SDLVideo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * TEST HARNESS ONLY (never shipped). Minecraft 26.3 asks SDL for an sRGB-capable default
 * framebuffer; Xvfb's software GLX has none, so the window cannot be created on a headless box.
 * When BACKROOMS_HEADLESS_GL is set we simply do not ask. The game renders into its own
 * framebuffers, so screenshots are unaffected.
 */
@Mixin(value = GlBackend.class, remap = false)
public abstract class HeadlessGlBackendMixin {
	private static final int SDL_GL_FRAMEBUFFER_SRGB_CAPABLE = 22;

	@Redirect(method = "createWindow", at = @At(value = "INVOKE", target = "Lorg/lwjgl/sdl/SDLVideo;SDL_GL_SetAttribute(II)Z"))
	private boolean backroomsTest$skipSrgbOnHeadless(int attribute, int value) {
		if (attribute == SDL_GL_FRAMEBUFFER_SRGB_CAPABLE && System.getenv("BACKROOMS_HEADLESS_GL") != null) {
			return true;
		}
		return SDLVideo.SDL_GL_SetAttribute(attribute, value);
	}
}
