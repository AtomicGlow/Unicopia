package com.minelittlepony.unicopia.item;

import java.util.List;

import javax.annotation.Nullable;

import com.minelittlepony.unicopia.EquinePredicates;
import com.minelittlepony.unicopia.entity.SpellcastEntity;
import com.minelittlepony.unicopia.magic.Affinity;
import com.minelittlepony.unicopia.magic.IDispenceable;
import com.minelittlepony.unicopia.magic.IMagicEffect;
import com.minelittlepony.unicopia.magic.IUseable;
import com.minelittlepony.unicopia.magic.items.ICastable;
import com.minelittlepony.unicopia.magic.spells.SpellCastResult;
import com.minelittlepony.unicopia.magic.spells.SpellRegistry;
import com.minelittlepony.util.VecHelper;

import net.minecraft.block.DispenserBlock;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.math.BlockPointer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.Hand;
import net.minecraft.util.Rarity;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class MagicGemItem extends Item implements ICastable {

    public MagicGemItem() {
        super(new Settings()
                .maxCount(16)
                .group(ItemGroup.BREWING));

        setDispenseable();
    }

    @Override
    public boolean hasEnchantmentGlint(ItemStack stack) {
        return super.hasEnchantmentGlint(stack) || SpellRegistry.stackHasEnchantment(stack);
    }

    @Override
    public SpellCastResult onDispenseSpell(BlockPointer source, ItemStack stack, IDispenceable effect) {
        Direction facing = source.getBlockState().get(DispenserBlock.FACING);
        BlockPos pos = source.getBlockPos().offset(facing);

        return effect.onDispenced(pos, facing, source, getAffinity(stack));
    }

    @Override
    public SpellCastResult onCastSpell(ItemUsageContext context, IMagicEffect effect) {
        if (effect instanceof IUseable) {
            return ((IUseable)effect).onUse(context, getAffinity(context.getStack()));
        }

        return SpellCastResult.PLACE;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {

        BlockPos pos = context.getBlockPos();
        Hand hand = context.getHand();
        PlayerEntity player = context.getPlayer();

        if (hand != Hand.MAIN_HAND || !EquinePredicates.MAGI.test(player)) {
            return ActionResult.PASS;
        }

        ItemStack stack = player.getStackInHand(hand);

        if (!SpellRegistry.stackHasEnchantment(stack)) {
            return ActionResult.FAIL;
        }

        IMagicEffect effect = SpellRegistry.instance().getSpellFrom(stack);

        if (effect == null) {
            return ActionResult.FAIL;
        }

        SpellCastResult result = onCastSpell(context, effect);

        if (!context.getWorld().isClient) {
            pos = pos.offset(context.getSide());

            if (result == SpellCastResult.PLACE) {
                castContainedSpell(context.getWorld(), pos, stack, effect).setOwner(player);
            }
        }

        if (result != SpellCastResult.NONE) {
            if (!player.isCreative()) {
                stack.decrement(1);
            }

            return ActionResult.SUCCESS;
        }

        return ActionResult.FAIL;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {

        ItemStack stack = player.getStackInHand(hand);

        if (!EquinePredicates.MAGI.test(player)) {
            return new TypedActionResult<>(ActionResult.PASS, stack);
        }

        if (!SpellRegistry.stackHasEnchantment(stack)) {
            return new TypedActionResult<>(ActionResult.FAIL, stack);
        }

        IUseable effect = SpellRegistry.instance().getUseActionFrom(stack);

        if (effect != null) {
            SpellCastResult result = effect.onUse(stack, getAffinity(stack), player, world, VecHelper.getLookedAtEntity(player, 5));

            if (result != SpellCastResult.NONE) {
                if (result == SpellCastResult.PLACE && !player.isCreative()) {
                    stack.decrement(1);
                }

                return new TypedActionResult<>(ActionResult.SUCCESS, stack);
            }
        }

        return new TypedActionResult<>(ActionResult.PASS, stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World worldIn, List<Text> tooltip, TooltipContext context) {
        if (SpellRegistry.stackHasEnchantment(stack)) {
            Affinity affinity = getAffinity(stack);

            Text text = new TranslatableText(String.format("%s.%s.tagline",
                    affinity.getTranslationKey(),
                    SpellRegistry.getKeyFromStack(stack)
            ));
            text.getStyle().setColor(affinity.getColourCode());

            tooltip.add(text);
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        if (SpellRegistry.stackHasEnchantment(stack)) {
            return new TranslatableText(getTranslationKey(stack) + ".enchanted.name",
                    new TranslatableText(String.format("%s.%s.name", getAffinity(stack).getTranslationKey(), SpellRegistry.getKeyFromStack(stack)
            )));
        }

        return super.getName(stack);
    }

    @Override
    public void appendStacks(ItemGroup tab, DefaultedList<ItemStack> subItems) {
        super.appendStacks(tab, subItems);

        if (isIn(tab)) {
            SpellRegistry.instance().getAllNames(getAffinity()).forEach(name -> {
                subItems.add(SpellRegistry.instance().enchantStack(new ItemStack(this), name));
            });
        }
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        if (SpellRegistry.stackHasEnchantment(stack)) {
            return Rarity.UNCOMMON;
        }

        return super.getRarity(stack);
    }

    @Override
    public boolean canFeed(SpellcastEntity entity, ItemStack stack) {
        IMagicEffect effect = entity.getEffect();

        return effect != null
                && entity.getAffinity() == getAffinity()
                && effect.getName().equals(SpellRegistry.getKeyFromStack(stack));
    }

    @Override
    public Affinity getAffinity() {
        return Affinity.GOOD;
    }
}
