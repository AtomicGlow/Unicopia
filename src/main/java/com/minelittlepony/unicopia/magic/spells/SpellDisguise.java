package com.minelittlepony.unicopia.magic.spells;

import java.util.UUID;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.minelittlepony.unicopia.Race;
import com.minelittlepony.unicopia.SpeciesList;
import com.minelittlepony.unicopia.UClient;
import com.minelittlepony.unicopia.UParticles;
import com.minelittlepony.unicopia.ability.IFlyingPredicate;
import com.minelittlepony.unicopia.ability.IHeightPredicate;
import com.minelittlepony.unicopia.entity.IOwned;
import com.minelittlepony.unicopia.entity.capabilities.IPlayer;
import com.minelittlepony.unicopia.magic.Affinity;
import com.minelittlepony.unicopia.magic.CasterUtils;
import com.minelittlepony.unicopia.magic.IAttachedEffect;
import com.minelittlepony.unicopia.magic.ICaster;
import com.minelittlepony.unicopia.magic.IMagicEffect;
import com.minelittlepony.unicopia.magic.ISuppressable;
import com.minelittlepony.unicopia.mixin.MixinEntity;
import com.minelittlepony.unicopia.projectile.ProjectileUtil;
import com.mojang.authlib.GameProfile;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.FlyingEntity;
import net.minecraft.entity.mob.ShulkerEntity;
import net.minecraft.entity.mob.VexEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ShulkerBulletEntity;
import net.minecraft.entity.vehicle.MinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.MathHelper;

public class SpellDisguise extends AbstractSpell implements IAttachedEffect, ISuppressable, IFlyingPredicate, IHeightPredicate {

    @Nonnull
    private String entityId = "";

    @Nullable
    private Entity entity;

    @Nullable
    private CompoundTag entityNbt;

    private int suppressionCounter;

    @Override
    public String getName() {
        return "disguise";
    }

    @Override
    public boolean isCraftable() {
        return false;
    }

    @Override
    public Affinity getAffinity() {
        return Affinity.BAD;
    }

    @Override
    public int getTint() {
        return 0x19E48E;
    }

    @Override
    public boolean isVulnerable(ICaster<?> otherSource, IMagicEffect other) {
        return suppressionCounter <= otherSource.getCurrentLevel();
    }

    @Override
    public void onSuppressed(ICaster<?> otherSource) {
        suppressionCounter = 100;
        setDirty(true);
    }

    @Override
    public boolean getSuppressed() {
        return suppressionCounter > 0;
    }

    public Entity getDisguise() {
        return entity;
    }

    public SpellDisguise setDisguise(@Nullable Entity entity) {
        if (entity == this.entity) {
            entity = null;
        }
        this.entityNbt = null;
        this.entityId = "";

        removeDisguise();

        if (entity != null) {
            entityNbt = encodeEntityToNBT(entity);
            entityId = entityNbt.getString("id");
        }

        setDirty(true);

        return this;
    }

    protected void removeDisguise() {
        if (entity != null) {
            entity.remove();
            entity = null;
        }
    }

    protected CompoundTag encodeEntityToNBT(Entity entity) {
        CompoundTag entityNbt = new CompoundTag();

        if (entity instanceof PlayerEntity) {
            GameProfile profile = ((PlayerEntity)entity).getGameProfile();

            entityNbt.putString("id", "player");
            entityNbt.putUuid("playerId", profile.getId());
            entityNbt.putString("playerName", profile.getName());

            CompoundTag tag = new CompoundTag();

            entity.saveToTag(tag);

            entityNbt.put("playerNbt", tag);
        } else {
            entity.saveToTag(entityNbt);
        }

        return entityNbt;
    }

    protected synchronized void createPlayer(CompoundTag nbt, GameProfile profile, ICaster<?> source) {
        removeDisguise();

        entity = UClient.instance().createPlayer(source.getEntity(), profile);
        entity.setCustomName(source.getOwner().getName());
        ((PlayerEntity)entity).fromTag(nbt.getCompound("playerNbt"));
        entity.setUuid(UUID.randomUUID());
        entity.extinguish();

        onEntityLoaded(source);
    }

    protected void checkAndCreateDisguiseEntity(ICaster<?> source) {
        if (entity == null && entityNbt != null) {
            CompoundTag nbt = entityNbt;
            entityNbt = null;

            if ("player".equals(entityId)) {
                createPlayer(nbt, new GameProfile(
                        nbt.getUuid("playerId"),
                        nbt.getString("playerName")
                    ), source);
                new Thread(() -> createPlayer(nbt, SkullBlockEntity.updateGameProfile(new GameProfile(
                    null,
                    nbt.getString("playerName")
                )), source)).start();
            } else {
                entity = EntityType.loadEntityWithPassengers(nbt, source.getWorld(), e -> {
                    e.extinguish();

                    return e;
                });
            }

            onEntityLoaded(source);
        }
    }

    protected void onEntityLoaded(ICaster<?> source) {
        if (entity == null) {
            return;
        }

        CasterUtils.toCaster(entity).ifPresent(c -> c.setEffect(null));

        if (source.isClient()) {
            source.getWorld().spawnEntity(entity);
        }
    }

    protected void copyBaseAttributes(LivingEntity from, Entity to) {

        // Set first because position calculations rely on it
        to.removed = from.removed;
        to.onGround = from.onGround;

        if (isAttachedEntity(entity)) {
            to.x = Math.floor(from.x) + 0.5;
            to.y = Math.floor(from.y);
            to.z = Math.floor(from.z) + 0.5;

            to.prevX = to.x;
            to.prevY = to.y;
            to.prevZ = to.z;

            to.prevRenderX = to.x;
            to.prevRenderY = to.x;
            to.prevRenderZ = to.x;

            to.setPosition(to.x, to.y, to.z);
        } else {
            to.copyPositionAndRotation(from);

            to.prevX = from.prevX;
            to.prevY = from.prevY;
            to.prevZ = from.prevZ;

            to.prevRenderX = from.prevRenderX;
            to.prevRenderY = from.prevRenderY;
            to.prevRenderZ = from.prevRenderZ;
        }

        if (to instanceof PlayerEntity) {
            PlayerEntity l = (PlayerEntity)to;

            l.field_7500 = l.x;
            l.field_7521 = l.y;
            l.field_7499 = l.z;
        }

        to.setVelocity(from.getVelocity());

        to.prevPitch = from.prevPitch;
        to.prevYaw = from.prevYaw;

        to.distanceWalkedOnStepModified = from.distanceWalkedOnStepModified;
        to.distanceWalkedModified = from.distanceWalkedModified;
        to.prevDistanceWalkedModified = from.prevDistanceWalkedModified;

        if (to instanceof LivingEntity) {
            LivingEntity l = (LivingEntity)to;

            l.rotationYawHead = from.rotationYawHead;
            l.prevRotationYawHead = from.prevRotationYawHead;
            l.renderYawOffset = from.renderYawOffset;
            l.prevRenderYawOffset = from.prevRenderYawOffset;

            l.limbSwing = from.limbSwing;
            l.limbSwingAmount = from.limbSwingAmount;
            l.prevLimbSwingAmount = from.prevLimbSwingAmount;

            l.swingingHand = from.swingingHand;
            l.swingProgress = from.swingProgress;
            l.swingProgressInt = from.swingProgressInt;
            l.isSwingInProgress = from.isSwingInProgress;

            l.hurtTime = from.hurtTime;
            l.deathTime = from.deathTime;
            l.setHealth(from.getHealth());

            for (EquipmentSlot i : EquipmentSlot.values()) {
                ItemStack neu = from.getEquippedStack(i);
                ItemStack old = l.getEquippedStack(i);
                if (old != neu) {
                    l.setEquippedStack(i, neu);
                }
            }
        }

        if (to instanceof IRangedAttackMob) {
            ItemStack activeItem = from.getActiveItemStack();

            ((IRangedAttackMob)to).setSwingingArms(!activeItem.isEmpty() && activeItem.getUseAction() == UseAction.BOW);
        }

        if (to instanceof TameableEntity) {
            ((TameableEntity)to).setSitting(from.isSneaking());
        }

        if (from.age < 100 || from instanceof PlayerEntity && ((PlayerEntity)from).isCreative()) {
            to.extinguish();
        }

        if (to.isOnFire()) {
            from.setOnFireFor(1);
        } else {
            from.extinguish();
        }

        to.setSneaking(from.isSneaking());
    }

    @Override
    public boolean updateOnPerson(ICaster<?> caster) {
        return update(caster);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean update(ICaster<?> source) {
        LivingEntity owner = source.getOwner();

        if (getSuppressed()) {
            suppressionCounter--;

            owner.setInvisible(false);
            if (source instanceof IPlayer) {
                ((IPlayer)source).setInvisible(false);
            }

            if (entity != null) {
                entity.setInvisible(true);
                entity.y = Integer.MIN_VALUE;
            }

            return true;
        }

        checkAndCreateDisguiseEntity(source);

        if (owner == null) {
            return true;
        }

        if (entity == null) {
            if (source instanceof IPlayer) {
                owner.setInvisible(false);
                ((IPlayer) source).setInvisible(false);
            }

            return false;
        }

        entity.noClip = true;
        entity.updateBlocked = true;

        entity.getEntityData().setBoolean("disguise", true);

        if (entity instanceof LivingEntity) {
            ((LivingEntity)entity).setNoAI(true);
        }

        entity.setInvisible(false);
        entity.setNoGravity(true);

        copyBaseAttributes(owner, entity);

        if (!skipsUpdate(entity)) {
            entity.tick();
        }

        if (entity instanceof ShulkerEntity) {
            ShulkerEntity shulker = ((ShulkerEntity)entity);

            shulker.yaw = 0;
            shulker.renderYawOffset = 0;
            shulker.prevRenderYawOffset = 0;

            shulker.setAttachmentPos(null);

            if (source.isClient() && source instanceof IPlayer) {
                IPlayer player = (IPlayer)source;


                float peekAmount = 0.3F;

                if (!owner.isSneaking()) {
                    float speed = (float)Math.sqrt(Math.pow(owner.motionX, 2) + Math.pow(owner.motionZ, 2));

                    peekAmount = MathHelper.clamp(speed * 30, 0, 1);
                }

                peekAmount = player.getInterpolator().interpolate("peek", peekAmount, 5);

                shulker.setPeekAmount(peekAmount);
            }
        }

        if (entity instanceof MinecartEntity) {
            entity.yaw += 90;
            entity.pitch = 0;
        }

        if (source instanceof IPlayer) {
            IPlayer player = (IPlayer)source;

            player.setInvisible(true);
            source.getOwner().setInvisible(true);

            if (entity instanceof IOwned) {
                IOwned.cast(entity).setOwner(player.getOwner());
            }

            if (entity instanceof PlayerEntity) {
                entity.getDataTracker().set(MixinEntity.Player.getModelFlag(), owner.getDataTracker().get(MixinEntity.Player.getModelFlag()));
            }

            if (player.isClientPlayer() && UClient.instance().getViewMode() == 0) {
                entity.setInvisible(true);
                entity.y = Integer.MIN_VALUE;
            }

            return player.getSpecies() == Race.CHANGELING;
        }

        return !source.getOwner().removed;
    }

    @Override
    public void setDead() {
        super.setDead();
        removeDisguise();
    }

    @Override
    public void render(ICaster<?> source) {
        if (getSuppressed()) {
            source.spawnParticles(UParticles.UNICORN_MAGIC, 5);
            source.spawnParticles(UParticles.CHANGELING_MAGIC, 5);
        } else if (source.getWorld().random.nextInt(30) == 0) {
            source.spawnParticles(UParticles.CHANGELING_MAGIC, 2);
        }
    }

    @Override
    public void toNBT(CompoundTag compound) {
        super.toNBT(compound);

        compound.putInt("suppressionCounter", suppressionCounter);
        compound.putString("entityId", entityId);

        if (entityNbt != null) {
            compound.put("entity", entityNbt);
        } else if (entity != null) {
            compound.put("entity", encodeEntityToNBT(entity));
        }
    }

    @Override
    public void fromNBT(CompoundTag compound) {
        super.fromNBT(compound);

        suppressionCounter = compound.getInt("suppressionCounter");

        String newId = compound.getString("entityId");

        if (!newId.contentEquals(entityId)) {
            entityNbt = null;
            removeDisguise();
        }

        if (compound.containsKey("entity")) {
            entityId = newId;

            entityNbt = compound.getCompound("entity");

            if (entity != null) {
                entity.fromTag(entityNbt);
            }
        }
    }

    @Override
    public boolean checkCanFly(IPlayer player) {
        if (entity == null || !player.getSpecies().canFly()) {
            return false;
        }

        if (entity instanceof IOwned) {
            IPlayer iplayer = SpeciesList.instance().getPlayer(((IOwned<PlayerEntity>)entity).getOwner());

            return iplayer != null && iplayer.getSpecies().canFly();
        }

        return entity instanceof FlyingEntity
                || entity instanceof AmbientEntity
                || entity instanceof EnderDragonEntity
                || entity instanceof VexEntity
                || entity instanceof ShulkerBulletEntity
                || ProjectileUtil.isProjectile(entity);
    }

    @Override
    public float getTargetEyeHeight(IPlayer player) {
        if (entity != null && !getSuppressed()) {
            if (entity instanceof FallingBlockEntity) {
                return 0.5F;
            }
            return entity.getStandingEyeHeight();
        }
        return -1;
    }

    @Override
    public float getTargetBodyHeight(IPlayer player) {
        if (entity != null && !getSuppressed()) {
            if (entity instanceof FallingBlockEntity) {
                return 0.9F;
            }
            return entity.getHeight() - 0.1F;
        }
        return -1;
    }

    public static boolean skipsUpdate(Entity entity) {
        return entity instanceof FallingBlockEntity
            || entity instanceof AbstractDecorationEntity
            || entity instanceof PlayerEntity;
    }

    public static boolean isAttachedEntity(Entity entity) {
        return entity instanceof ShulkerEntity
            || entity instanceof AbstractDecorationEntity
            || entity instanceof FallingBlockEntity;
    }
}
