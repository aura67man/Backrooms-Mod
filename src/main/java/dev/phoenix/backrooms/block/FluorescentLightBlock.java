package dev.phoenix.backrooms.block;

import dev.phoenix.backrooms.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * A ceiling light panel. Panels generated with flickering=true stutter on and off at random:
 * a random tick starts a burst, scheduled ticks run it, and the burst re-arms itself.
 */
public class FluorescentLightBlock extends Block {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;
	public static final BooleanProperty FLICKERING = BooleanProperty.create("flickering");

	public FluorescentLightBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(LIT, true).setValue(FLICKERING, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(LIT, FLICKERING);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(FLICKERING);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIT) && !level.getBlockTicks().hasScheduledTick(pos, this)) {
			level.scheduleTick(pos, this, 1);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!state.getValue(FLICKERING)) {
			return;
		}
		boolean nowLit = !state.getValue(LIT);
		level.setBlock(pos, state.setValue(LIT, nowLit), Block.UPDATE_ALL);
		if (!nowLit) {
			level.playSound(null, pos, ModSounds.LIGHT_BUZZ, SoundSource.BLOCKS, 0.5F, 0.9F + random.nextFloat() * 0.3F);
			// usually a quick stutter, occasionally the tube stays dead for a few seconds
			level.scheduleTick(pos, this, random.nextInt(10) == 0 ? 40 + random.nextInt(80) : 1 + random.nextInt(6));
		} else if (random.nextInt(3) != 0) {
			level.scheduleTick(pos, this, 2 + random.nextInt(8)); // burst continues
		} else {
			level.scheduleTick(pos, this, 80 + random.nextInt(320)); // rest, then go again
		}
	}

	@Override
	public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
		if (state.getValue(LIT) && random.nextInt(14) == 0) {
			level.playLocalSound(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, ModSounds.LIGHT_BUZZ, SoundSource.BLOCKS, 0.12F, 0.8F + random.nextFloat() * 0.4F, false);
		}
	}
}
