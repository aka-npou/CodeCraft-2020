package older.v03.random.base;

import model.Entity;
import model.EntityType;
import model.PlayerView;

import java.util.ArrayList;
import java.util.List;

public class AllEntities {

    private int myId;

    private List<Entity> resources = new ArrayList<>();

    private List<Entity> myUnits = new ArrayList<>();
    private List<Entity> myAttackers = new ArrayList<>();
    private List<Entity> myBuildings = new ArrayList<>();

    private List<Entity> enemyUnits = new ArrayList<>();
    private List<Entity> enemyBuildings = new ArrayList<>();
    private List<Entity> enemyBuilders = new ArrayList<>();
    private List<Entity> myBuilders = new ArrayList<>();

    private int currentUnits;
    private int maxUnits;

    public AllEntities(PlayerView playerView) {
        myId = playerView.getMyId();

        currentUnits = 0;
        maxUnits = 0;
        for (Entity otherEntity : playerView.getEntities()) {
            if (otherEntity.getPlayerId() != null && otherEntity.getPlayerId() == playerView.getMyId()) {
                currentUnits += otherEntity.getProperties().getPopulationUse();
                maxUnits += otherEntity.getProperties().getPopulationProvide();
            }
        }

        for (Entity entity : playerView.getEntities()) {
            if (entity.getEntityType() == EntityType.RESOURCE) {
                resources.add(entity);
                continue;
            }
            if (entity.getPlayerId() == myId) {
                if (entity.getEntityType().isBuilding()) {
                    myBuildings.add(entity);
                }
                if (entity.getEntityType().isUnit()) {
                    if (entity.getEntityType() == EntityType.BUILDER_UNIT) {
                        myBuilders.add(entity);
                    }
                    myUnits.add(entity);
                }
                if (entity.getProperties().getAttack() != null && entity.getProperties().getAttack().getDamage() > 0) {
                    myAttackers.add(entity);
                }

            } else {
                if (entity.getEntityType().isBuilding()) {
                    enemyBuildings.add(entity);
                }
                if (entity.getEntityType().isUnit()) {
                    if (entity.getEntityType() == EntityType.BUILDER_UNIT) {
                        enemyBuilders.add(entity);
                    }
                    enemyUnits.add(entity);
                }
            }
        }
    }

    public int getMyId() {
        return myId;
    }

    public List<Entity> getResources() {
        return resources;
    }

    public List<Entity> getEnemyUnits() {
        return enemyUnits;
    }

    public List<Entity> getEnemyBuilders() {
        return enemyBuilders;
    }

    public List<Entity> getMyBuilders() {
        return myBuilders;
    }

    public List<Entity> getMyUnits() {
        return myUnits;
    }

    public List<Entity> getMyAttackers() {
        return myAttackers;
    }

    public List<Entity> getEnemyBuildings() {
        return enemyBuildings;
    }

    public List<Entity> getMyBuildings() {
        return myBuildings;
    }

    public int getCurrentUnits() {
        return currentUnits;
    }

    public int getMaxUnits() {
        return maxUnits;
    }
}
