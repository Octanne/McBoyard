package eu.octanne.mcboyard.modules.excalibur;

import eu.octanne.mcboyard.entity.CrochetEntity;
import eu.octanne.mcboyard.entity.MiddleEntity;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.entity.Entity;

import java.util.*;

public class StandKey {

    private static List<StandKey> standKeys = new ArrayList<>();

    private double[] locStand;
    private String locWorld;

    private CrochetEntity[] crochetEntities;

    private MiddleEntity middleEntity;

    private final UUID id;
    private boolean isComplete;
    private boolean toUpdate = false;

    public StandKey(String locWorld, double locX, double locY, double locZ) {
        this.locStand = new double[]{locX, locY, locZ};
        this.locWorld = locWorld;

        constructCrochetEntities();
        setBlocks();

        id = UUID.randomUUID();
        isComplete = true;
    }

    public StandKey(UUID id) {
        constructCrochetEntities();
        setBlocks();

        this.id = id;
        isComplete = false;
    }

    public static void clearStandKeys() {
        standKeys.clear();
    }

    public static void markAllStandToUpdate() {
        for (StandKey standKey : standKeys) {
            standKey.toUpdate = true;
        }
    }

    public boolean attachMiddleEntity(MiddleEntity middleEntity) {
        if (isComplete) return false;
        this.middleEntity = middleEntity;
        this.locWorld = middleEntity.getWorld().getWorld().getName();
        this.locStand = new double[]{
                middleEntity.getBukkitEntity().getLocation().getX(),
                middleEntity.getBukkitEntity().getLocation().getY(),
                middleEntity.getBukkitEntity().getLocation().getZ()
        };
        if (this.crochetEntities[0] != null && this.crochetEntities[1] != null
                && this.crochetEntities[2] != null && this.crochetEntities[3] != null) {
            isComplete = true;
        }

        return true;
    }

    public boolean attachCrochetEntity(CrochetEntity crochetEntity) {
        if (isComplete) return false;
        for (int i = 0; i < crochetEntities.length; i++) {
            if (crochetEntities[i] == null) {
                crochetEntities[i] = crochetEntity;
                break;
            }
        }
        if (this.crochetEntities[0] != null && this.crochetEntities[1] != null
                && this.crochetEntities[2] != null && this.crochetEntities[3] != null) {
            isComplete = true;
        }
        return true;
    }

    public boolean detachCrochetEntity(CrochetEntity crochetEntity) {
        for (int i = 0; i < crochetEntities.length; i++) {
            if (crochetEntities[i] != null && crochetEntities[i].getStandKeyID().equals(crochetEntity.getStandKeyID())) {
                crochetEntities[i] = null;
                break;
            }
        }
        if (this.crochetEntities[0] == null || this.crochetEntities[1] == null
                || this.crochetEntities[2] == null || this.crochetEntities[3] == null) {
            isComplete = false;
        }
        return true;
    }

    public boolean detachMiddleEntity() {
        this.middleEntity = null;
        isComplete = false;
        return true;
    }

    public boolean isComplete() {
        return isComplete;
    }

    public UUID getID() {
        return id;
    }

    public Location getBukkitLocation() {
        return new Location(Bukkit.getWorld(locWorld), locStand[0], locStand[1], locStand[2]);
    }

    public void despawn() {
        for (CrochetEntity crochetEntity : crochetEntities) {
            crochetEntity.despawn();
        }
        middleEntity.despawn();

        for (Location loc : getBlocksLoc()) {
            loc.getBlock().setType(org.bukkit.Material.AIR);
        }
    }

    public void respawn() {
        for (CrochetEntity crochetEntity : crochetEntities) {
            crochetEntity.despawn();
        }
        middleEntity.despawn();
        constructCrochetEntities();
        setBlocks();
    }

    public boolean entityIsStandKey(Entity entity) {
        if (((CraftEntity)entity).getHandle() instanceof MiddleEntity ||
                ((CraftEntity)entity).getHandle() instanceof CrochetEntity) {
            net.minecraft.server.v1_16_R3.Entity nmsEntity = ((CraftEntity)entity).getHandle();
            if (middleEntity.getBukkitEntity().equals(entity) ||
                    crochetEntities[0].getBukkitEntity().equals(entity) ||
                    crochetEntities[1].getBukkitEntity().equals(entity) ||
                    crochetEntities[2].getBukkitEntity().equals(entity) ||
                    crochetEntities[3].getBukkitEntity().equals(entity)) {
                return true;
            } else return false;
        } else return false;
    }

    private void constructCrochetEntities() {
        crochetEntities = new CrochetEntity[4];
        // TODO construct crochet entities
    }

    public List<Location> getBlocksLoc() {
        List<Location> locs = new ArrayList<>();
        // TODO get blocks loc
        return locs;
    }

    private void setBlocks() {
        // TODO setblock of crochet & middle stand
    }

    public void delete() {
        standKeys.remove(this);
        despawn();
    }

    public static List<StandKey> getStandKeys() {
        return standKeys;
    }

    public static Optional<StandKey> getStandKey(UUID id) {
        for (StandKey standKey : standKeys) {
            if (standKey.id.equals(id)) {
                return Optional.of(standKey);
            }
        }
        return Optional.empty();
    }

    public static StandKey getStandKeyRegenIfNotLoad(UUID idStandKey) {
        Optional<StandKey> standKey = getStandKey(idStandKey);
        if (standKey.isPresent()) {
            return standKey.get();
        } else {
            StandKey standKey1 = new StandKey(idStandKey);
            standKeys.add(standKey1);
            return standKey1;
        }
    }

    public static boolean removeStandKey(UUID standKeyID) {
        Optional<StandKey> standKey = getStandKey(standKeyID);
        if (standKey.isPresent()) {
            standKey.get().despawn();
            standKeys.remove(standKey.get());
            return true;
        } else return false;
    }

    public static Optional<StandKey> createStandKey(String locWorld, double locX, double locY, double locZ) {
        // check if standkey is already exist
        for (StandKey standKey : standKeys) {
            if (standKey.locWorld.equals(locWorld) && standKey.locStand[0] == locX
                    && standKey.locStand[1] == locY && standKey.locStand[2] == locZ) {
                return Optional.empty();
            }
        }

        StandKey standKey = new StandKey(locWorld, locX, locY, locZ);
        standKeys.add(standKey);
        return Optional.of(standKey);
    }

    public boolean isUpdate() {
        return !toUpdate;
    }

    public boolean updateStandKeyInstance() {
        if (toUpdate && !standKeys.contains(this)) {
            toUpdate = false;
            standKeys.add(this);
            return true;
        } else {
            return false;
        }
    }
}
