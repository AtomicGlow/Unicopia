package com.minelittlepony.unicopia.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.entity.mob.ShulkerEntity;

@Mixin(ShulkerEntity.class)
public interface MixinShulkerEntity {
    @Accessor
    float getOpenProgress();

    @Accessor
    void setOpenProgress(float value);

    @Accessor
    float getPrevOpenProgress();

    @Accessor
    void setPrevOpenProgress(float value);

    @Invoker("getPeekAmount")
    int callGetPeekAmount();

    @Invoker("setPeekAmount")
    void callSetPeekAmount(int amount);
}
