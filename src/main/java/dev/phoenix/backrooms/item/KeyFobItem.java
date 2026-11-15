package dev.phoenix.backrooms.item;

import dev.phoenix.backrooms.registry.ModDimensions;
import dev.phoenix.backrooms.registry.ModSounds;
import dev.phoenix.backrooms.transit.Transit;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Entrance 3 (craftable, reliable): a key fob for a car you do not own. Press it in the real
 * world and you are standing in Sublevel P looking for it; press it in any liminal level and it takes you home.
 */
public class KeyFobItem extends Item {
	private static final int COOLDOWN_TICKS = 60;

	public KeyFobItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
			serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.KEY_FOB_BEEP, SoundSource.PLAYERS, 1.0F, 1.0F);
			player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
			if (ModDimensions.isLiminal(level)) {
				Transit.leave(serverPlayer);
			} else if (!Transit.enter(serverPlayer, ModDimensions.CARPARK, player.blockPosition())) {
				serverPlayer.sendOverlayMessage(Component.translatable("message.backrooms.nowhere"));
			}
		}
		return InteractionResult.SUCCESS;
	}
}
