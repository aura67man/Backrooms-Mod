package dev.phoenix.backrooms.entity;

import dev.phoenix.backrooms.BackroomsConfig;
import dev.phoenix.backrooms.registry.ModSounds;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathType;

/**
 * Tube Moth: a pale moth the size of a hand that drifts between ceiling lights. Scenery, almost
 * always. Swat one, though, and it and every moth nearby will come for you (for half a heart a bite).
 */
public class TubeMothEntity extends PathfinderMob {
	public TubeMothEntity(EntityType<? extends TubeMothEntity> type, Level level) {
		super(type, level);
		this.moveControl = new FlyingMoveControl<>(this, 20, true);
		this.setPathfindingMalus(PathType.WATER, -1.0F);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
			.add(Attributes.MAX_HEALTH, 4.0)
			.add(Attributes.FLYING_SPEED, 0.5)
			.add(Attributes.MOVEMENT_SPEED, 0.25)
			.add(Attributes.ATTACK_DAMAGE, 1.0)
			.add(Attributes.FOLLOW_RANGE, 16.0);
	}

	public static boolean checkSpawnRules(EntityType<TubeMothEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		BackroomsConfig config = BackroomsConfig.get();
		return config.tubeMothEnabled && (reason != EntitySpawnReason.NATURAL || random.nextDouble() < config.tubeMothSpawnRate)
			&& Mob.checkMobSpawnRules(type, level, reason, pos, random);
	}

	@Override
	protected PathNavigation createNavigation(Level level) {
		FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
		navigation.setCanOpenDoors(false);
		navigation.setCanFloat(true);
		return navigation;
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3, true));
		this.goalSelector.addGoal(2, new SeekLightGoal(this));
		this.goalSelector.addGoal(3, new WaterAvoidingRandomFlyingGoal(this, 1.0));
		this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
	}

	@Override
	public void tick() {
		if (this.level() instanceof ServerLevel && !BackroomsConfig.get().tubeMothEnabled) {
			this.discard();
			return;
		}
		super.tick();
	}

	@Override
	protected void checkFallDamage(double ya, boolean onGround, BlockState onState, BlockPos pos) {
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.MOTH_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 80;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.MOTH_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.MOTH_DEATH;
	}

	@Override
	protected float getSoundVolume() {
		return 0.5F;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
	}

	/** Every so often, pick the nearest bright block overhead and go and bother it. */
	private static final class SeekLightGoal extends Goal {
		private final TubeMothEntity moth;
		private BlockPos light;

		private SeekLightGoal(TubeMothEntity moth) {
			this.moth = moth;
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}

		@Override
		public boolean canUse() {
			if (this.moth.getTarget() != null || this.moth.getRandom().nextInt(40) != 0) {
				return false;
			}
			this.light = this.findLight();
			return this.light != null;
		}

		@Override
		public void start() {
			RandomSource random = this.moth.getRandom();
			this.moth.getNavigation().moveTo(this.light.getX() + 0.5 + (random.nextDouble() - 0.5) * 3.0, this.light.getY() - 1.2,
				this.light.getZ() + 0.5 + (random.nextDouble() - 0.5) * 3.0, 1.0);
		}

		@Override
		public boolean canContinueToUse() {
			return this.moth.getTarget() == null && !this.moth.getNavigation().isDone();
		}

		private BlockPos findLight() {
			BlockPos origin = this.moth.blockPosition();
			BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
			BlockPos best = null;
			double bestDistance = Double.MAX_VALUE;
			for (int dx = -8; dx <= 8; dx++) {
				for (int dz = -8; dz <= 8; dz++) {
					for (int dy = 0; dy <= 8; dy++) {
						cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
						double distance = cursor.distSqr(origin);
						if (distance < bestDistance && this.moth.level().getBlockState(cursor).getLightEmission() >= 12) {
							best = cursor.immutable();
							bestDistance = distance;
						}
					}
				}
			}
			return best;
		}
	}
}
