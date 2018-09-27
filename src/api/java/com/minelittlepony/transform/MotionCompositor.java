package com.minelittlepony.transform;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;

import com.minelittlepony.util.math.MathUtil;

public abstract class MotionCompositor {

    protected double calculateRoll(EntityPlayer player, double motionX, double motionY, double motionZ) {

        // since model roll should probably be calculated from model rotation rather than entity rotation...
        double roll = MathUtil.sensibleAngle(player.prevRenderYawOffset - player.renderYawOffset);
        double horMotion = Math.sqrt(motionX * motionX + motionZ * motionZ);
        float modelYaw = MathUtil.sensibleAngle(player.renderYawOffset);

        // detecting that we're flying backwards and roll must be inverted
        if (Math.abs(MathUtil.sensibleAngle((float) Math.toDegrees(Math.atan2(motionX, motionZ)) + modelYaw)) > 90) {
            roll *= -1;
        }

        // ayyy magic numbers (after 5 - an approximation of nice looking coefficients calculated by hand)

        // roll might be zero, in which case Math.pow produces +Infinity. Anything x Infinity = NaN.
        double pow = roll != 0 ? Math.pow(Math.abs(roll), -0.191) : 0;

        roll *= horMotion * 5 * (3.6884f * pow);

        assert !Float.isNaN((float)roll);

        return MathHelper.clamp(roll, -54, 54);
    }

    protected double calculateIncline(EntityPlayer player, double motionX, double motionY, double motionZ) {
        double dist = Math.sqrt(motionX * motionX + motionZ * motionZ);
        double angle = Math.atan2(motionY, dist);

        if (!player.capabilities.isFlying) {
            angle /= 2;
        }

        angle = MathUtil.clampLimit(angle, Math.PI / 3);

        return Math.toDegrees(angle);
    }

}
