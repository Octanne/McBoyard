package eu.octanne.mcboyard.modules.maitika;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.v1_16_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftCaveSpider;
import org.bukkit.entity.CaveSpider;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;

import net.minecraft.server.v1_16_R3.AxisAlignedBB;
import net.minecraft.server.v1_16_R3.ChatComponentText;
import net.minecraft.server.v1_16_R3.DamageSource;
import net.minecraft.server.v1_16_R3.Entity;
import net.minecraft.server.v1_16_R3.EntityCaveSpider;
import net.minecraft.server.v1_16_R3.EntityLiving;
import net.minecraft.server.v1_16_R3.EntityLlamaSpit;
import net.minecraft.server.v1_16_R3.EntityTypes;
import net.minecraft.server.v1_16_R3.MathHelper;
import net.minecraft.server.v1_16_R3.MobEffect;
import net.minecraft.server.v1_16_R3.MobEffects;
import net.minecraft.server.v1_16_R3.MovingObjectPositionEntity;
import net.minecraft.server.v1_16_R3.SoundEffects;
import net.minecraft.server.v1_16_R3.World;

public class MaitikaEntity extends EntityCaveSpider {
    public enum MaitikaAttackState {
        VANILLA,
        THROW,
        ON_PLAYER
    }

    private MaitikaAttackState attackState = MaitikaAttackState.VANILLA;
    private int ticksUntilNewState = 0;

    public MaitikaEntity(World world, Location loc) {
        super(EntityTypes.CAVE_SPIDER, world);
        this.setPosition(loc.getX(), loc.getY(), loc.getZ());
        this.setYawPitch(loc.getYaw(), loc.getPitch());
        addScoreboardTag("maitika");
        world.addEntity(this);
        setCustomName(new ChatComponentText("Maïtika"));
        setCustomNameVisible(true);
        craftAttributes.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(1000);
        setHealth(1000);
        craftAttributes.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.5);

        moveAvoidBlocks();
    }

    public MaitikaEntity(Location loc) {
        this(((CraftWorld) loc.getWorld()).getHandle(), loc);
    }

    @Override
    public boolean isClimbing() {
        // Empêche l'araignée de grimper
        return false;
    }

    public static List<CraftCaveSpider> getMaitikaEntities(org.bukkit.World world) {
        return world.getEntitiesByClasses(CaveSpider.class)
                .stream()
                .filter(entity -> entity.getScoreboardTags().contains("maitika"))
                .map(entity -> (CraftCaveSpider) entity)
                .toList();
    }

    @Override
    public boolean attackEntity(Entity target) {
        onTickDamage();
        switch (attackState) {
            case VANILLA:
                return super.attackEntity(target);
            case THROW:
                // Throw a llama spit
                throwSpit(target);
                return false;
            case ON_PLAYER:
                if (target instanceof EntityLiving) {
                    EntityLiving entityLiving = (EntityLiving) target;
                    if (!this.isPassenger()) {
                        this.startRiding(entityLiving);
                    }
                    entityLiving.addEffect(new MobEffect(MobEffects.POISON, 25, 0));
                    super.attackEntity(target);
                    return true;
                }
                return false;
        }
        return false;
    }

    @Override
    public boolean damageEntity(DamageSource damagesource, float f) {
        onTickDamage();
        return super.damageEntity(damagesource, f);
    }

    /**
     * When the spider attacks or is attacked, change its attack state randomly
     */
    private void onTickDamage() {
        ticksUntilNewState--;
        if (ticksUntilNewState <= 0) {
            setAttackState(MaitikaAttackState.values()[(int) (Math.random() * MaitikaAttackState.values().length)]);
            ticksUntilNewState = (int) (Math.random() * 9) + 1; // Entre 1 et 10 coups
        }
    }

    public void setAttackState(MaitikaAttackState attackState) {
        if (this.attackState == MaitikaAttackState.ON_PLAYER) {
            this.stopRiding();
            moveAvoidBlocks();
        }

        this.attackState = attackState;

        if (this.attackState == MaitikaAttackState.ON_PLAYER) {
            Entity target = this.getGoalTarget();
            if (target == null)
                return;
            this.startRiding(target);
        }
    }

    public void throwSpit(@NotNull Entity target) {
        MaitikaEntity maitika = this;
        EntityLlamaSpit spit = new EntityLlamaSpit(EntityTypes.LLAMA_SPIT, getWorld()) {
            @Override
            protected void a(MovingObjectPositionEntity target) {
                target.getEntity().damageEntity(DamageSource.a(this, maitika).c(), 4.0F);
            }
        };
        spit.setPosition(locX(), locY() + 0.5, locZ());
        spit.setShooter(this);

        double dX = target.locX() - spit.locX();
        double dY = target.e(1. / 3) - spit.locY();
        double dZ = target.locZ() - spit.locZ();
        float f = MathHelper.sqrt(dX * dX + dZ * dZ) * 0.2F;
        spit.shoot(dX, dY + f, dZ, 1.0F, 10.0F);
        float r1 = random.nextFloat();
        float r2 = random.nextFloat();
        this.world.playSound(null, locX(), locY(), locZ(),
                SoundEffects.ENTITY_LLAMA_SPIT, this.getSoundCategory(),
                0.5F, 1.8F + (r1 - r2) * 0.2F);
        this.world.addEntity(spit);
    }

    /**
     * Avoid glitching into blocks
     */
    public void moveAvoidBlocks() {
        AxisAlignedBB idbox = getBoundingBox();
        Block[] blocks = new Block[4];
        double xMin = idbox.minX;
        double xMax = idbox.maxX;
        double zMin = idbox.minZ;
        double zMax = idbox.maxZ;
        blocks[0] = new Location(getWorld().getWorld(), idbox.minX, idbox.minY, idbox.minZ).getBlock();
        blocks[1] = blocks[0].getRelative(0, 0, 1);
        blocks[2] = blocks[0].getRelative(1, 0, 0);
        blocks[3] = blocks[0].getRelative(1, 0, 1);

        if (blocks[0].getBoundingBox().contains(idbox.minX, idbox.minY, idbox.minZ)) {
            BoundingBox box = blocks[0].getBoundingBox();
            xMin = Math.max(xMin, box.getMaxX());
            zMin = Math.max(zMin, box.getMaxZ());
        }
        if (blocks[1].getBoundingBox().contains(idbox.minX, idbox.minY, idbox.maxZ)) {
            BoundingBox box = blocks[1].getBoundingBox();
            xMin = Math.max(xMin, box.getMaxX());
            zMax = Math.min(zMax, box.getMinZ());
        }
        if (blocks[2].getBoundingBox().contains(idbox.maxX, idbox.minY, idbox.minZ)) {
            BoundingBox box = blocks[2].getBoundingBox();
            xMax = Math.min(xMax, box.getMinX());
            zMin = Math.max(zMin, box.getMaxZ());
        }
        if (blocks[3].getBoundingBox().contains(idbox.maxX, idbox.minY, idbox.maxZ)) {
            BoundingBox box = blocks[3].getBoundingBox();
            xMax = Math.min(xMax, box.getMinX());
            zMax = Math.min(zMax, box.getMinZ());
        }

        double xMid = (xMin + xMax) / 2;
        double zMid = (zMin + zMax) / 2;
        setPosition(xMid, locY(), zMid);
    }
}
