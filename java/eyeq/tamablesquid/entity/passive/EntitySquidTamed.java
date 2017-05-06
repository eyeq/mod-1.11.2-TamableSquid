package eyeq.tamablesquid.entity.passive;

import java.util.UUID;

import com.google.common.base.Optional;
import eyeq.util.entity.*;
import eyeq.util.entity.player.EntityPlayerUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumDyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.EnumHand;
import net.minecraft.world.World;

public class EntitySquidTamed extends EntitySquid implements IEntityLeashable, IUEntityOwnable, IEntityRideablePlayer {
    protected static final DataParameter<Optional<UUID>> OWNER_ID = EntityDataManager.createKey(EntitySquidTamed.class, DataSerializers.OPTIONAL_UNIQUE_ID);

    private EntityPlayer ridingPlayer;

    public EntitySquidTamed(World world) {
        super(world);
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        this.dataManager.register(OWNER_ID, Optional.absent());
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(20.0);
    }

    @Override
    public void writeEntityToNBT(NBTTagCompound compound) {
        super.writeEntityToNBT(compound);
        if(this.getOwnerId() == null) {
            compound.setString("OwnerUUID", "");
        } else {
            compound.setString("OwnerUUID", this.getOwnerId().toString());
        }
    }

    @Override
    public void readEntityFromNBT(NBTTagCompound compound) {
        super.readEntityFromNBT(compound);
        if(compound.hasKey("OwnerUUID", 8)) {
            this.setOwnerId(UUID.fromString(compound.getString("OwnerUUID")));
        }
    }

    @Override
    public UUID getOwnerId() {
        return (UUID) ((Optional) this.dataManager.get(OWNER_ID)).orNull();
    }

    @Override
    public void setOwnerId(UUID ownerId) {
        this.dataManager.set(OWNER_ID, Optional.fromNullable(ownerId));
    }

    @Override
    public EntityLivingBase getOwner() {
        try {
            UUID uuid = this.getOwnerId();
            return uuid == null ? null : this.world.getPlayerEntityByUUID(uuid);
        } catch(IllegalArgumentException var2) {
            return null;
        }
    }

    @Override
    protected void updateLeashedState() {
        super.updateLeashedState();
        EntityLivingUtils.updateLeashedState(this);
    }

    @Override
    public double followLeashSpeed() {
        return 1.0;
    }

    @Override
    public void onLeashDistance(float distance) {
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public boolean shouldDismountInWater(Entity rider) {
        return false;
    }

    @Override
    public double getYOffset() {
        if(ridingPlayer != null) {
            return super.getYOffset() + 0.8F;
        }
        if(this.getRidingEntity() == null) {
            return super.getYOffset();
        }
        return super.getYOffset() + this.getRidingEntity().getEyeHeight();
    }

    @Override
    public void onUpdate() {
        if(ridingPlayer != null) {
            if(ridingPlayer.isDead) {
                EntityPlayerUtils.PASSENGER.remove(ridingPlayer);
                ridingPlayer = null;
            } else {
                if(!world.isRemote) {
                    if(isRiding()) {
                        dismountRidingEntity();
                    }
                }
                EntityUtils.setRidingEntity(this, ridingPlayer);
            }
        }
        if(ridingPlayer == null) {
            super.onUpdate();
            return;
        }
        // updateRidden
        this.motionX = 0.0;
        this.motionY = 0.0;
        this.motionZ = 0.0;
        super.onUpdate();
        this.prevOnGroundSpeedFactor = this.onGroundSpeedFactor;
        this.onGroundSpeedFactor = 0.0F;
        this.fallDistance = 0.0F;
        this.setPosition(ridingPlayer.posX, ridingPlayer.posY + ridingPlayer.getMountedYOffset() + this.getYOffset(), ridingPlayer.posZ);
        EntityUtils.setRidingEntity(this, null);
    }

    @Override
    public void onLivingUpdate() {
        if(this.getRidingEntity() != null || ridingPlayer != null) {
            this.squidPitch = 0;
            if(this.getAir() < 0) {
                this.setAir(0);
            }
        }
        super.onLivingUpdate();
    }

    @Override
    public boolean processInteract(EntityPlayer player, EnumHand hand) {
        ItemStack itemStack = player.getHeldItem(hand);
        if(itemStack.isEmpty() && hand == EnumHand.MAIN_HAND) {
            if(ridingPlayer == player) {
                EntityPlayerUtils.PASSENGER.remove(player);
                ridingPlayer = null;
                return true;
            }
            Entity riddenEntity;
            if(EntityPlayerUtils.PASSENGER.containsKey(player)) {
                riddenEntity = EntityPlayerUtils.PASSENGER.get(player);
                if(riddenEntity.isDead) {
                    EntityPlayerUtils.PASSENGER.remove(player);
                    riddenEntity = player;
                } else {
                    while(!riddenEntity.getPassengers().isEmpty()) {
                        riddenEntity = riddenEntity.getPassengers().get(0);
                    }
                }
            } else {
                riddenEntity = player;
            }
            if(riddenEntity instanceof EntityPlayer) {
                EntityPlayerUtils.PASSENGER.put(((EntityPlayer) riddenEntity), this);
                ridingPlayer = (EntityPlayer) riddenEntity;
            } else {
                if(!world.isRemote) {
                    this.startRiding(riddenEntity);
                }
            }
            return true;
        }
        if(itemStack.getItem() == Items.DYE && itemStack.getItemDamage() == EnumDyeColor.BLACK.getDyeDamage()) {
            if(!world.isRemote) {
                player.startRiding(this);
            }
            return true;
        }
        return false;
    }

    @Override
    public EntityPlayer getRidingPlayer() {
        return ridingPlayer;
    }
}
