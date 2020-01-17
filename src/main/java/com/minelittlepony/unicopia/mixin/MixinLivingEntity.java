package com.minelittlepony.unicopia.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.minelittlepony.unicopia.ducks.IRaceContainerHolder;
import com.minelittlepony.unicopia.entity.IEntity;
import com.minelittlepony.unicopia.entity.capabilities.LivingEntityCapabilities;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;

@Mixin(LivingEntity.class)
public abstract class MixinLivingEntity extends Entity implements IRaceContainerHolder<IEntity> {

    private final IEntity caster = createRaceContainer();

    private MixinLivingEntity() { super(null, null); }

    @Override
    public IEntity createRaceContainer() {
        return new LivingEntityCapabilities((LivingEntity)(Object)this);
    }

    @Override
    public IEntity getRaceContainer() {
        return caster;
    }

    @Inject(method = "canSee(Lnet/minecraft/entity/Entity)Z", at = @At("HEAD"))
    private void onCanSee(Entity other, CallbackInfoReturnable<Boolean> info) {
        if (caster.isInvisible()) {
            info.setReturnValue(false);
        }
    }

    @Inject(method = "method_6040()V", at = @At("HEAD"))
    protected void onFinishUsing(CallbackInfo info) {
        LivingEntity self = (LivingEntity)(Object)this;

        if (!self.getActiveItem().isEmpty() && self.isUsingItem()) {
            caster.onUse(self.getActiveItem());
        }
    }

    @Inject(method = "jump()V", at = @At("RETURN"))
    private void onJump(CallbackInfo info) {
        caster.onJump();
    }

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void beforeTick(CallbackInfo info) {
        caster.beforeUpdate();
    }

    @Inject(method = "tick()V", at = @At("RETURN"))
    private void afterTick(CallbackInfo info) {
        caster.onUpdate();
    }

    @Inject(method = "<clinit>()V", at = @At("RETURN"))
    private static void clinit(CallbackInfo info) {
        LivingEntityCapabilities.boostrap();
    }
}
