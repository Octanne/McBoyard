package eu.octanne.mcboyard.modules.grandeporte;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import eu.octanne.mcboyard.McBoyard;
import eu.octanne.mcboyard.modules.grandeporte.GrandePorteModule.PORTES;

public class GrandePorte {
    private final PORTES porte;
    private ArmorStand entityLeft = null;
    private ArmorStand entityRight = null;
    private BukkitTask taskAnimation = null;
    private boolean isOpen = false;
    private int animationRotation = 0; // From 0 to 130 (ANIMATION_TICKS)
    private static final float PIVOT_OFFSET = 0.68f;
    private static final short ANIMATION_TICK_OFFSET = 40;
    private static final short ANIMATION_TICKS = 90 + ANIMATION_TICK_OFFSET;

    public GrandePorte(PORTES porte) {
        this.porte = porte;
        setIsOpen(false);
        spawnEntities();
    }

    public PORTES getPorte() {
        return porte;
    }

    private float getClosedYaw(PORTES porte) {
        switch (porte) {
            case PORTE_OUEST_0:
                return 90f;
            default:
                return 0f;
        }
    }

    /**
     * Get the location of the left pivot of the door
     * The entity has an offset of 0.68 blocks
     */
    private Location getLeftPivotLocation() {
        World world = McBoyard.getWorld();
        float yaw = getClosedYaw(porte);
        switch (porte) {
            case PORTE_OUEST_0:
                return new Location(world, -54.68, 74, 25.32, yaw, 0f);
            default:
                return null;
        }
    }

    /**
     * Get the location of the right pivot of the door
     * The entity has an offset of -0.68 blocks
     */
    private Location getRightPivotLocation() {
        World world = McBoyard.getWorld();
        float yaw = getClosedYaw(porte);
        switch (porte) {
            case PORTE_OUEST_0:
                return new Location(world, -54.68, 74, 28.68, yaw, 0f);
            default:
                return null;
        }
    }

    /**
     * Get the location of the right door
     * The left door has an opening animation 40 ticks after the right door
     *
     * @param rotation The rotation of the door (0 to 130)
     */
    private Location getLeftLocation(int rotation) {
        rotation = Math.max(0, rotation - ANIMATION_TICK_OFFSET);
        Location loc = getLeftPivotLocation();
        loc.setYaw(loc.getYaw() - rotation - 90f);
        loc.add(loc.getDirection().multiply(PIVOT_OFFSET));
        loc.setYaw(loc.getYaw() + 90f);
        return loc;
    }

    /**
     * Get the location of the left door
     *
     * @param rotation The rotation of the door (0 to 130)
     */
    private Location getRightLocation(int rotation) {
        rotation = Math.min(rotation, 90);
        Location loc = getRightPivotLocation();
        loc.setYaw(loc.getYaw() + rotation + 90f);
        loc.add(loc.getDirection().multiply(PIVOT_OFFSET));
        loc.setYaw(loc.getYaw() - 90f);
        return loc;
    }

    /**
     * Detect the entities of the doors
     * @param force Force the detection of the entities or keep existing ones
     */
    private void detectEntities(boolean force) {
        if (force || (entityLeft == null || !entityLeft.isValid())) {
            Location locLeft = getLeftPivotLocation();
            ArmorStand entity = locLeft.getWorld()
                                    .getNearbyEntitiesByType(ArmorStand.class, locLeft, 0.8)
                                    .stream()
                                    .filter(e -> e.getScoreboardTags().contains("fb_gp_2"))
                                    .findFirst()
                                    .orElse(null);
            if (entity != null) {
                entityLeft = entity;
            }
        }
        if (force || (entityRight == null || !entityRight.isValid())) {
            Location locRight = getRightPivotLocation();
            ArmorStand entity = locRight.getWorld()
                                    .getNearbyEntitiesByType(ArmorStand.class, locRight, 0.8)
                                    .stream()
                                    .filter(e -> e.getScoreboardTags().contains("fb_gp_1"))
                                    .findFirst()
                                    .orElse(null);
            if (entity != null) {
                entityRight = entity;
            }
        }
        // Detect if the door is open
        if (force && entityLeft != null) {
            float currentYawLeft = entityLeft.getLocation().getYaw();
            float closedYaw = getClosedYaw(porte);
            setIsOpen(currentYawLeft != closedYaw);
        }
    }

    private ItemStack getDoorItem() {
        ItemStack item = new ItemStack(Material.GHAST_TEAR);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(12000);
        item.setItemMeta(meta);
        return item;
    }

    private void spawnEntities() {
        detectEntities(true);
        if (entityLeft == null || !entityLeft.isValid()) {
            Location locLeft = getLeftLocation(0);
            entityLeft = locLeft.getWorld().spawn(locLeft, ArmorStand.class);
            entityLeft.setInvisible(true);
            entityLeft.setInvulnerable(true);
            entityLeft.setGravity(false);
            entityLeft.addScoreboardTag("fb_gp_2");
            EntityEquipment equipment = entityLeft.getEquipment();
            equipment.setHelmet(getDoorItem());
        }
        if (entityRight == null || !entityRight.isValid()) {
            Location locRight = getRightLocation(0);
            entityRight = locRight.getWorld().spawn(locRight, ArmorStand.class);
            entityRight.setInvisible(true);
            entityRight.setInvulnerable(true);
            entityRight.setGravity(false);
            entityRight.addScoreboardTag("fb_gp_1");
            EntityEquipment equipment = entityRight.getEquipment();
            equipment.setHelmet(getDoorItem());
        }
    }

    private void setIsOpen(boolean open) {
        isOpen = open;
        if (open) {
            animationRotation = ANIMATION_TICKS;
        } else {
            animationRotation = 0;
        }
    }

    private void setRotations(int animationRotation) {
        detectEntities(false);
        if (entityLeft != null && entityLeft.isValid()) {
            Location locLeft = getLeftLocation(animationRotation);
            entityLeft.teleport(locLeft);
        }
        if (entityRight != null && entityRight.isValid()) {
            Location locRight = getRightLocation(animationRotation);
            entityRight.teleport(locRight);
        }
    }

    public void placeOpen() {
        stopTaskAnimation();
        setRotations(ANIMATION_TICKS);
        setIsOpen(true);
    }

    public void placeClose() {
        stopTaskAnimation();
        setRotations(0);
        setIsOpen(false);
    }

    private void stopTaskAnimation() {
        if (taskAnimation != null) {
            taskAnimation.cancel();
            taskAnimation = null;
        }
    }

    public void stopAnimation() {
        if (isOpen) {
            placeOpen();
        } else {
            placeClose();
        }
    }

    private void startAnimation(boolean open) {
        if (taskAnimation != null) {
            taskAnimation.cancel();
            taskAnimation = null;
        }
        isOpen = open;
        taskAnimation = Bukkit.getScheduler().runTaskTimer(McBoyard.instance, () -> {
            if (open) {
                animationRotation++;
            } else {
                animationRotation--;
            }

            if (animationRotation <= 0) {
                placeClose();
            } else if (animationRotation >= ANIMATION_TICKS) {
                placeOpen();
            }

            setRotations(animationRotation);
        }, 1, 1);
    }

    public void reset() {
        spawnEntities();
    }

    public void open() {
        if (isOpen)
            return;
        startAnimation(true);
    }

    public void close() {
        if (!isOpen)
            return;
        startAnimation(false);
    }

    public void toggle() {
        if (isOpen) {
            close();
        } else {
            open();
        }
    }
}
