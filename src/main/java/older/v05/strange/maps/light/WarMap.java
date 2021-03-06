package older.v05.strange.maps.light;

import model.Coordinate;
import model.Entity;
import model.EntityType;
import model.PlayerView;
import older.v05.strange.Constants;
import older.v05.strange.collections.AllEntities;
import older.v05.strange.maps.EntitiesMap;
import util.DebugInterface;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class WarMap {

    public static final int RANGER_RANGE = 5;
    private Set<Coordinate> enemyBuildingLocations = new HashSet<>(128);
    private Set<Coordinate> enemyUnitLocations = new HashSet<>(128);
    private Set<Coordinate> enemyWorkerLocations = new HashSet<>(128);
    private Set<Coordinate> myAttackersLocations = new HashSet<>(128);
    private int[][] enemyDistanceMap;
    private int[][] enemyWorkerDistanceMap;
    private int[][] dominanceMap;
    private int mapSize;
    private EntitiesMap entitiesMap;
    private AllEntities allEntities;
    private VisibilityMap visibility;
    private VirtualResources resources;
    private int tick;

    public WarMap(VisibilityMap visibility, VirtualResources resources) {
        this.visibility = visibility;
        this.resources = resources;
    }

    public int getTick() {
        return tick;
    }

    public void checkTick(PlayerView playerView) {
        if (getTick() != playerView.getCurrentTick()) {
            throw new RuntimeException("visibility is not initialized");
        }
    }

    public void init(
            PlayerView playerView,
            EntitiesMap entitiesMap,
            AllEntities allEntities
    ) {
        tick = playerView.getCurrentTick();
        this.entitiesMap = entitiesMap;
        this.allEntities = allEntities;

        visibility.checkTick(playerView);
        resources.checkTick(playerView);

        if (mapSize == 0) {
            if (playerView.isOneOnOne()) {
                enemyBuildingLocations.add(new Coordinate(72, 72));
            } else {
                enemyBuildingLocations.add(new Coordinate(72, 72));
                enemyBuildingLocations.add(new Coordinate(7, 72));
                enemyBuildingLocations.add(new Coordinate(72, 7));
            }
            mapSize = playerView.getMapSize();
        }

        myAttackersLocations = new HashSet<>(128);
        for (Entity attacker : allEntities.getMyAttackers()) {
            myAttackersLocations.add(attacker.getPosition());
        }


        Iterator<Coordinate> buildingsIterator = enemyBuildingLocations.iterator();
        while (buildingsIterator.hasNext()) {
            Coordinate next = buildingsIterator.next();
            if (visibility.isVisible(next)) {
                Entity entity = entitiesMap.getEntity(next);
                if (!entity.isBuilding() || entity.isMy()) {
                    buildingsIterator.remove(); // clean old instances
                }
            }
        }
        Iterator<Coordinate> unitsIterator = enemyUnitLocations.iterator();
        while (unitsIterator.hasNext()) {
            Coordinate next = unitsIterator.next();
            if (visibility.isVisible(next)) {
                Entity entity = entitiesMap.getEntity(next);
                if (!entity.isUnit() || entity.isMy()) { // clean old instances
                    unitsIterator.remove();
                }
            }
        }
        Iterator<Coordinate> workersIterator = enemyWorkerLocations.iterator();
        while (workersIterator.hasNext()) {
            Coordinate next = workersIterator.next();
            if (visibility.isVisible(next)) {
                Entity entity = entitiesMap.getEntity(next);
                if (entity.getEntityType() != EntityType.BUILDER_UNIT || entity.isMy()) { // clean old instances
                    workersIterator.remove();
                }
            }
        }

        for (Entity building : allEntities.getEnemyBuildings()) {
            enemyBuildingLocations.add(building.getPosition());
        }

        for (Entity unit : allEntities.getEnemyUnits()) {
            enemyUnitLocations.add(unit.getPosition());
            if (unit.getEntityType() == EntityType.BUILDER_UNIT) {
                enemyWorkerLocations.add(unit.getPosition());
            }
        }

        Set<Coordinate> enemyDominance = new HashSet<>(128);
        enemyDominance.addAll(enemyUnitLocations);
        enemyDominance.addAll(enemyBuildingLocations);

//        Set<Coordinate> myDominance = allEntities.getMyEntities().stream().map(Entity::getPosition).collect(Collectors.toSet());

        fillEnemyDistances(enemyDominance);
        fillMyAttackersDistances(myAttackersLocations);
        fillEnemyWorkerDistances(enemyWorkerLocations);
    }

    private boolean isPassable(Coordinate coordinate) {
        Entity entity = this.entitiesMap.getEntity(coordinate);
        return !(entity.isBuilding() && entity.isMy())/* && !entity.isMy(EntityType.BUILDER_UNIT)*/;
    }

    private boolean isBuilder(Coordinate coordinate) {
        return this.entitiesMap.getEntity(coordinate).isMy(EntityType.BUILDER_UNIT);
    }

    private void fillEnemyWorkerDistances(Set<Coordinate> coordinateList) {
        enemyWorkerDistanceMap = new int[mapSize][mapSize];
        int[][] delayFuseForCalculation = new int[mapSize][mapSize];
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.isInBounds()
                        && enemyWorkerDistanceMap[coordinate.getX()][coordinate.getY()] == 0
                        && (isPassable(coordinate) || i <= RANGER_RANGE + 1)) {
                    if (i <= RANGER_RANGE + 1) {
                        // skip attack range
                        enemyWorkerDistanceMap[coordinate.getX()][coordinate.getY()] = i;
                        coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                        coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                        coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                        coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                        continue;
                    }
                    int resourceCount = resources.getResourceCount(coordinate);
                    if (resourceCount > 0 && delayFuseForCalculation[coordinate.getX()][coordinate.getY()] == 0) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()] = resourceCount / 5 + 1; // magic
                    }
                    if (delayFuseForCalculation[coordinate.getX()][coordinate.getY()] > 1) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()]--;
                        coordinateListNext.add(coordinate);
                    } else {
                        enemyWorkerDistanceMap[coordinate.getX()][coordinate.getY()] = i;
                        if (!isBuilder(coordinate)) {
                            coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                        }
                    }
                }
            }
            coordinateList = coordinateListNext;

            if (i > Constants.MAX_CYCLES) {
                if (DebugInterface.isDebugEnabled()) {
                    throw new RuntimeException("protection from endless cycles");
                } else {
                    break;
                }
            }
        }
    }

    private void fillEnemyDistances(Set<Coordinate> coordinateList) {
        enemyDistanceMap = new int[mapSize][mapSize];
        int[][] delayFuseForCalculation = new int[mapSize][mapSize];
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.isInBounds()
                        && enemyDistanceMap[coordinate.getX()][coordinate.getY()] == 0
                        && isPassable(coordinate)) {
                    int resourceCount = resources.getResourceCount(coordinate);
                    if (resourceCount > 0 && delayFuseForCalculation[coordinate.getX()][coordinate.getY()] == 0) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()] = resourceCount / 5 + 1; // magic
                    }
                    if (delayFuseForCalculation[coordinate.getX()][coordinate.getY()] > 1) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()]--;
                        coordinateListNext.add(coordinate);
                    } else {
                        enemyDistanceMap[coordinate.getX()][coordinate.getY()] = i;
                        if (!isBuilder(coordinate)) {
                            coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                        }
                    }
                }
            }
            coordinateList = coordinateListNext;

            if (i > Constants.MAX_CYCLES) {
                if (DebugInterface.isDebugEnabled()) {
                    throw new RuntimeException("protection from endless cycles");
                } else {
                    break;
                }
            }
        }
    }

    private void fillMyAttackersDistances(Set<Coordinate> coordinateList) {
        dominanceMap = new int[mapSize][mapSize];
        int[][] delayFuseForCalculation = new int[mapSize][mapSize];
        for (int i = 1; !coordinateList.isEmpty(); i++) {
            Set<Coordinate> coordinateListNext = new HashSet<>(128);
            for (Coordinate coordinate : coordinateList) {
                if (coordinate.isInBounds()
                        && dominanceMap[coordinate.getX()][coordinate.getY()] == 0
                        && isPassable(coordinate)) {
                    int resourceCount = resources.getResourceCount(coordinate);
                    if (resourceCount > 0 && delayFuseForCalculation[coordinate.getX()][coordinate.getY()] == 0) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()] = resourceCount / 5 + 1; // magic
                    }
                    if (delayFuseForCalculation[coordinate.getX()][coordinate.getY()] > 1) {
                        delayFuseForCalculation[coordinate.getX()][coordinate.getY()]--;
                        coordinateListNext.add(coordinate);
                    } else {
                        dominanceMap[coordinate.getX()][coordinate.getY()] = i;
                        if (!isBuilder(coordinate)) {
                            coordinateListNext.add(new Coordinate(coordinate.getX() - 1, coordinate.getY() + 0));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() + 1));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 0, coordinate.getY() - 1));
                            coordinateListNext.add(new Coordinate(coordinate.getX() + 1, coordinate.getY() + 0));
                        }
                    }
                }
            }
            coordinateList = coordinateListNext;

            if (i > Constants.MAX_CYCLES) {
                if (DebugInterface.isDebugEnabled()) {
                    throw new RuntimeException("protection from endless cycles");
                } else {
                    break;
                }
            }
        }
    }

    public Coordinate getMinOfTwoPositions(Coordinate old, Coordinate newPosition) {
        if (newPosition.isOutOfBounds()) {
            return old;
        }
        int newDistance = getDistanceToEnemy(newPosition);
        if (newDistance == 0) {
            return old;
        }
        int oldDistance = getDistanceToEnemy(old);
        if (newDistance < oldDistance) {
            return newPosition;
        }

/*
        if (newDistance == oldDistance && entitiesMap.isEmpty(newPosition)) { // scatter
            return newPosition;
        }
*/
        return old;
    }

    public Coordinate getMinOfTwoPositionsForRangedUnit(Coordinate old, Coordinate newPosition) {
        if (newPosition.isOutOfBounds()) {
            return old;
        }
        int newDistance = getDistanceToEnemy(newPosition);
        int distanceToEnemyWorker = getDistanceToEnemyWorker(newPosition);
        if (distanceToEnemyWorker > 0 && distanceToEnemyWorker < newDistance) {
            newDistance = distanceToEnemyWorker;
        }
        if (newDistance == 0) {
            return old;
        }

        int oldDistance = getDistanceToEnemy(old);
        int oldDistanceToEnemyWorker = getDistanceToEnemyWorker(old);
        if (oldDistanceToEnemyWorker > 0 && oldDistanceToEnemyWorker < oldDistance) {
            oldDistance = oldDistanceToEnemyWorker;
        }
        if (newDistance < oldDistance) {
            return newPosition;
        }

/*
        if (newDistance == oldDistance && entitiesMap.isEmpty(newPosition)) { // scatter
            return newPosition;
        }
*/
        return old;
    }

    public int getDistanceToEnemy(Coordinate position) {
        return getDistanceToEnemy(position.getX(), position.getY());
    }

    public int getDistanceToEnemy(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return enemyDistanceMap[x][y];
    }

    public int getDistanceToEnemyWorker(Coordinate position) {
        return getDistanceToEnemyWorker(position.getX(), position.getY());
    }

    public int getDistanceToEnemyWorker(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return enemyWorkerDistanceMap[x][y];
    }

    public int getDistanceToGoodGuys(Coordinate position) {
        return getDistanceToGoodGuys(position.getX(), position.getY());
    }

    public int getDistanceToGoodGuys(int x, int y) {
        if (x < 0 || y < 0 || x >= mapSize || y >= mapSize) {
            return 0;
        }
        return dominanceMap[x][y];
    }

    public Coordinate getPositionClosestToEnemy(Coordinate from) {
        int radius = 1;
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                from = getMinOfTwoPositions(from, new Coordinate(from.getX() + i, from.getY() + j));
            }
        }
        return from;
    }

    public Coordinate getPositionClosestToForRangedUnit(Coordinate from) {
        int radius = 1;
        for (int i = -radius; i <= radius; i++) {
            for (int j = -radius; j <= radius; j++) {
                from = getMinOfTwoPositionsForRangedUnit(from, new Coordinate(from.getX() + i, from.getY() + j));
            }
        }
        return from;
    }

    public Coordinate getPositionClosestToEnemy(Coordinate from, List<Coordinate> coordinateList) {
        Coordinate position = from;
        for (Coordinate newPosition : coordinateList) {
            position = getMinOfTwoPositions(position, newPosition);
        }
        return position;
    }
}
