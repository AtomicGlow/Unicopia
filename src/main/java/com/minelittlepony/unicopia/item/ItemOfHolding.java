package com.minelittlepony.unicopia.item;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import com.minelittlepony.unicopia.EquinePredicates;
import com.minelittlepony.unicopia.UClient;
import com.minelittlepony.unicopia.inventory.gui.ContainerOfHolding;
import com.minelittlepony.unicopia.inventory.gui.InventoryOfHolding;
import com.minelittlepony.unicopia.magic.items.IMagicalItem;
import com.minelittlepony.unicopia.spell.SpellAffinity;
import com.minelittlepony.util.VecHelper;

import net.minecraft.client.util.ITooltipFlag;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.IInteractionObject;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemOfHolding extends Item implements IMagicalItem {

    public ItemOfHolding(String domain, String name) {
        setCreativeTab(CreativeTabs.TRANSPORTATION);
        setTranslationKey(name);
        setRegistryName(domain, name);
        setMaxStackSize(1);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);

        Map<String, Integer> counts = new HashMap<>();

        InventoryOfHolding.iterateContents(stack, (i, itemstack) -> {
            String name = itemstack.getDisplayName();

            counts.put(name, counts.getOrDefault(name, 0) + itemstack.getCount());
            return true;
        });

        for (String name : counts.keySet()) {
            tooltip.add(String.format("%s x%d", name, counts.get(name)));
        }
    }

    @Override
    public TypedActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, EnumHand hand) {

        if (!EquinePredicates.MAGI.test(player)) {
            return super.onItemRightClick(world, player, hand);
        }

        ItemStack stack = player.getStackInHand(hand);

        if (player.isSneaking()) {
            RayTraceResult hit = VecHelper.getObjectMouseOver(player, 5, 0);

            if (hit != null) {
                if (hit.typeOfHit == RayTraceResult.Type.BLOCK) {
                    BlockPos pos = hit.getBlockPos();

                    TileEntity tile = world.getTileEntity(pos);

                    if (tile instanceof IInventory) {
                        InventoryOfHolding inventory = InventoryOfHolding.getInventoryFromStack(stack);

                        inventory.addBlockEntity(world, pos, (TileEntity & IInventory)tile);
                        inventory.writeTostack(stack);
                        inventory.closeInventory(player);

                        return new TypedActionResult<>(EnumActionResult.SUCCESS, stack);
                    }

                    Box box = new Box(pos.offset(hit.sideHit)).grow(0.5);

                    List<Entity> itemsAround = world.getEntitiesInAABBexcluding(player, box, EquinePredicates.ITEMS);

                    if (itemsAround.size() > 0) {
                        InventoryOfHolding inventory = InventoryOfHolding.getInventoryFromStack(stack);

                        inventory.addItem((EntityItem)itemsAround.get(0));
                        inventory.writeTostack(stack);
                        inventory.closeInventory(player);

                        return new TypedActionResult<>(EnumActionResult.SUCCESS, stack);
                    }
                }
            }

            return new TypedActionResult<>(EnumActionResult.FAIL, stack);
        }

        UClient.instance().displayGuiToPlayer(player, new Inventory(stack));

        player.playSound(SoundEvents.BLOCK_ENDERCHEST_OPEN, 0.5F, 1);

        return new TypedActionResult<>(EnumActionResult.SUCCESS, stack);
    }

    @Override
    public SpellAffinity getAffinity() {
        return SpellAffinity.NEUTRAL;
    }

    @Override
    public boolean hasInnerSpace() {
        return true;
    }

    public class Inventory implements IInteractionObject {

        private String customname = null;

        Inventory(ItemStack stack) {
            if (stack.hasDisplayName()) {
                customname = stack.getDisplayName();
            }
        }

        @Override
        public String getName() {
            return "unicopi.gui.title.itemofholding";
        }

        @Override
        public boolean hasCustomName() {
            return customname != null;
        }

        @Override
        public ITextComponent getDisplayName() {
            if (hasCustomName()) {
                return new TextComponentString(customname);
            }
            return new TextComponentTranslation(getName());
        }

        @Override
        public Container createContainer(InventoryPlayer playerInventory, PlayerEntity player) {
            return new ContainerOfHolding(player);
        }

        @Override
        public String getGuiID() {
            return "unicopia:itemofholding";
        }
    }
}
