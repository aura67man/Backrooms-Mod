package dev.phoenix.backrooms.entity;

import dev.phoenix.backrooms.BackroomsConfig;
import dev.phoenix.backrooms.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Fixture: something that stands exactly like a floor lamp for as long as anybody is looking
 * at it. Slow, patient, and only ever closer when you look back. It cannot move, turn or be
 * shoved while any player has it in view; the moment nobody does, it drags itself towards the nearest one.
 */
public class FixtureEntity extends Monster {
	private static final EntityDataAccessor<Boolean> WATCHED = SynchedEntityData.defineId(FixtureEntity.class, EntityDataSerializers.BOOLEAN);
	private static final double WATCH_RANGE = 64.0;
	private static final double MIN_SPACING = 64.0;
	/** Roughly a 60 degree half-angle: on screen means watched, peripheral vision included. */
	private static final double WATCH_CONE = 0.5;

	public FixtureEntity(EntityType<? extends FixtureEntity> type, Level level) {
		super(type, level);
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Monster.createMonsterAttributes()
			.add(Attributes.MAX_HEALTH, 30.0)
			.add(Attributes.MOVEMENT_SPEED, 0.2)
			.add(Attributes.ATTACK_DAMAGE, 6.0)
			.add(Attributes.FOLLOW_RANGE, 40.0)
			.add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
	}

	public static boolean checkSpawnRules(EntityType<FixtureEntity> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
		BackroomsConfig config = BackroomsConfig.get();
		if (!config.fixtureEnabled || level.getDifficulty() == Difficulty.PEACEFUL || !Mob.checkMobSpawnRules(type, level, reason, pos, random)) {
			return false;
		}
		if (reason != EntitySpawnReason.NATURAL) {
			return true;
		}
		// A stalker is only frightening if it is rare: thin out attempts by the configured rate and
		// never let two Fixtures spawn within MIN_SPACING blocks of each other.
		return random.nextDouble() < config.fixtureSpawnRate
			&& level.getEntitiesOfClass(FixtureEntity.class, new AABB(pos).inflate(MIN_SPACING), fixture -> true).isEmpty();
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(WATCHED, false);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, true));
		this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.7));
		// randomInterval 0: pick a target the very first tick nobody is looking, there may not be many of those
		this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false, null));
	}

	public boolean isWatched() {
		return this.entityData.get(WATCHED);
	}

	@Override
	public void tick() {
		if (this.level() instanceof ServerLevel level) {
			if (!BackroomsConfig.get().fixtureEnabled) {
				this.discard();
				return;
			}
			boolean watched = this.isAlive() && this.isSeenByAnyPlayer(level);
			if (watched != this.isWatched()) {
				this.entityData.set(WATCHED, watched);
			}
			if (watched) {
				this.getNavigation().stop();
				this.setDeltaMovement(0.0, Math.min(0.0, this.getDeltaMovement().y), 0.0);
			}
		}
		super.tick();
	}

	private boolean isSeenByAnyPlayer(ServerLevel level) {
		for (ServerPlayer player : level.players()) {
			if (!player.isSpectator() && player.isAlive() && player.distanceToSqr(this) < WATCH_RANGE * WATCH_RANGE
				&& this.isLookingAtMe(player, WATCH_CONE, false, true, this.getEyeY(), this.getY() + this.getBbHeight() * 0.5, this.getY() + 0.2)) {
				return true;
			}
		}
		return false;
	}

	/** While watched the AI does not run at all: no goals, no pathing, no head turning. */
	@Override
	protected boolean isImmobile() {
		return super.isImmobile() || this.isWatched();
	}

	@Override
	public boolean isPushable() {
		return !this.isWatched() && super.isPushable();
	}

	/** Fixtures do not care how bright it is. */
	@Override
	public float getWalkTargetValue(BlockPos pos, LevelReader level) {
		return 0.0F;
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		boolean hit = super.doHurtTarget(level, target);
		if (hit) {
			this.playSound(ModSounds.FIXTURE_ATTACK, 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
		}
		return hit;
	}

	@Override
	protected SoundEvent getAmbientSound() {
		return ModSounds.FIXTURE_AMBIENT;
	}

	@Override
	public int getAmbientSoundInterval() {
		return 140;
	}

	@Override
	protected SoundEvent getHurtSound(DamageSource source) {
		return ModSounds.FIXTURE_HURT;
	}

	@Override
	protected SoundEvent getDeathSound() {
		return ModSounds.FIXTURE_DEATH;
	}

	@Override
	protected void playStepSound(BlockPos pos, BlockState state) {
		this.playSound(ModSounds.FIXTURE_STEP, 0.7F, 0.85F + this.random.nextFloat() * 0.3F);
	}
}
