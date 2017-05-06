package eyeq.tamablesquid.entity.ai;

import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.util.math.MathHelper;

public class EntitySquidAIMoveRandom extends EntityAIBase {
    protected final EntitySquid entity;

    public EntitySquidAIMoveRandom(EntitySquid entity) {
        this.entity = entity;
    }

    @Override
    public boolean shouldExecute() {
        if(this.entity.getAge() > 100) {
            this.entity.setMovementVector(0.0F, 0.0F, 0.0F);
            return false;
        }
        return this.entity.getRNG().nextInt(50) == 0 || !this.entity.isInWater() || !this.entity.hasMovementVector();
    }

    @Override
    public void updateTask() {
        float f = this.entity.getRNG().nextFloat() * (float) Math.PI * 2.0F;
        float x = MathHelper.cos(f) * 0.2F;
        float y = -0.1F + this.entity.getRNG().nextFloat() * 0.2F;
        float z = MathHelper.sin(f) * 0.2F;
        this.entity.setMovementVector(x, y, z);
    }
}
