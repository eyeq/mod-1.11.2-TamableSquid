package eyeq.tamablesquid.event;

import eyeq.util.entity.EntityUtils;
import eyeq.util.entity.IUEntityOwnable;
import eyeq.util.entity.ai.UEntityAIDumpHeldItem;
import eyeq.util.entity.ai.UEntityAITempt;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.Item;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import eyeq.tamablesquid.entity.passive.EntitySquidTamed;
import eyeq.tamablesquid.entity.ai.EntitySquidAIMoveRandom;
import eyeq.util.entity.ai.UEntityAIEatHeldItemFood;
import eyeq.util.entity.ai.UEntityAIMoveToEntityItem;

import java.lang.reflect.InvocationTargetException;
import java.util.Random;

public class TamableSquidEventHandler {
    private boolean interact;

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if(event.getWorld().isRemote) {
            return;
        }
        if(event.getEntity() instanceof EntitySquid) {
            EntitySquid entity = (EntitySquid) event.getEntity();
            entity.tasks.taskEntries.clear();
            entity.tasks.addTask(1, new UEntityAIEatHeldItemFood(entity, (ItemFood) Items.FISH));
            entity.tasks.addTask(1, new UEntityAIMoveToEntityItem(entity, 0.2, 32.0F, Items.FISH));
            if(entity instanceof EntitySquidTamed) {
                entity.tasks.addTask(2, new UEntityAITempt(entity, 0.2, false, Items.FISH));
            }
            entity.tasks.addTask(3, new EntitySquidAIMoveRandom(entity));
            entity.tasks.addTask(4, new UEntityAIDumpHeldItem(entity, Items.FISH));
            entity.setDropChance(EntityEquipmentSlot.MAINHAND, 2.0F);
            entity.setDropChance(EntityEquipmentSlot.OFFHAND, 2.0F);
        }
    }

    @SubscribeEvent
    public void onLivingUpdate(LivingUpdateEvent event) {
        EntityLivingBase entity = event.getEntityLiving();
        World world = entity.getEntityWorld();
        if(world.isRemote || !(entity instanceof EntitySquid)) {
            return;
        }
        if(entity.isDead) {
            return;
        }
        for(EntityItem entityItem : world.getEntitiesWithinAABB(EntityItem.class, entity.getEntityBoundingBox().expand(1.0, 0.0, 1.0))) {
            ItemStack itemStack = entityItem.getEntityItem();
            if(!entityItem.isDead && !entityItem.cannotPickup()) {
                if(itemStack.getItem() == Items.FISH) {
                    ItemStack handItemStack = entity.getHeldItemMainhand();
                    if(handItemStack.getCount() < 1) {
                        entity.setHeldItem(EnumHand.MAIN_HAND, itemStack);
                    } else if(handItemStack.isItemEqual(itemStack)) {
                        handItemStack.grow(itemStack.getCount());
                    }
                    entity.onItemPickup(entityItem, itemStack.getCount());
                    entityItem.setDead();
                    break;
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerInteractEntity(PlayerInteractEvent.EntityInteract event) {
        EntityPlayer player = event.getEntityPlayer();
        Entity oldEntity = event.getTarget();
        World world = oldEntity.getEntityWorld();
        if(!(oldEntity instanceof EntitySquid)) {
            return;
        }
        EnumHand hand = event.getHand();
        ItemStack itemStack = player.getHeldItem(hand);
        Item item = itemStack.getItem();
        if(item != Items.FISH) {
            return;
        }
        if(oldEntity.getClass() == EntitySquid.class) {
            event.setCanceled(true);
            if(world.isRemote) {
                return;
            }
            EntitySquidTamed newEntity = new EntitySquidTamed(world);
            try {
                EntityUtils.copyDataFromOld(newEntity, oldEntity);
            } catch(InvocationTargetException | IllegalAccessException e) {
                e.printStackTrace();
            }
            newEntity.setOwnerId(player.getUniqueID());

            world.removeEntity(oldEntity);
            world.onEntityRemoved(oldEntity);
            oldEntity.isDead = true;

            world.spawnEntity(newEntity);
            return;
        }
        ItemStack handItemStack = ((EntitySquid) oldEntity).getHeldItemMainhand();
        if(handItemStack.getCount() < 1) {
            ((EntitySquid) oldEntity).setHeldItem(EnumHand.MAIN_HAND, new ItemStack(itemStack.getItem(), 1, itemStack.getMetadata()));
        } else if(itemStack.isItemEqual(handItemStack)) {
            handItemStack.grow(1);
        } else {
            return;
        }
        event.setCanceled(true);
        Random rand = ((EntitySquid) oldEntity).getRNG();
        double dx = rand.nextGaussian() * 0.02;
        double dy = rand.nextGaussian() * 0.02;
        double dz = rand.nextGaussian() * 0.02;
        world.spawnParticle(EnumParticleTypes.NOTE, oldEntity.posX, oldEntity.posY + oldEntity.height + 0.1, oldEntity.posZ, dx, dy, dz);
        if(world.isRemote) {
            return;
        }
        oldEntity.entityDropItem(new ItemStack(Items.DYE, 1, EnumDyeColor.BLACK.getDyeDamage()), 0.0F);
        // 寝取り
        if(oldEntity instanceof IUEntityOwnable) {
            ((IUEntityOwnable) oldEntity).setOwnerId(player.getUniqueID());
        }
        if(!player.isCreative()) {
            itemStack.shrink(1);
        }
    }
}
