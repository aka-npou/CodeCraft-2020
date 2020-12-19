package mystrategy.maps;

import model.Coordinate;
import model.EntityType;
import model.PlayerView;
import mystrategy.collections.AllEntities;
import mystrategy.maps.light.SimCityPlan;
import mystrategy.maps.light.WarMap;

import java.util.HashSet;
import java.util.Set;

public class SimCityMap {

    public static final int MIN_DISTANCE_TO_ENEMY = 15;
    private Coordinate[][] houseBuildCoordinates;
    private Coordinate[][] rangedBaseBuildCoordinates;
    private int mapSize;
    private boolean needBarracks;

    public SimCityMap(
            PlayerView playerView,
            EntitiesMap entitiesMap,
            AllEntities allEntities,
            WarMap warMap,
            SimCityPlan simCityPlan) {
        warMap.checkTick(playerView);
        mapSize = playerView.getMapSize();
        houseBuildCoordinates = new Coordinate[mapSize][mapSize];
        rangedBaseBuildCoordinates = new Coordinate[mapSize][mapSize];
        needBarracks = allEntities.getMyBuildings().stream()
                .noneMatch(ent -> ent.getProperties().getBuild() != null
                        && ent.getProperties().getBuild().getOptions()[0] == EntityType.RANGED_UNIT);

        int houseSize = playerView.getEntityProperties().get(EntityType.HOUSE).getSize();
        int houseSizeWithMargin = houseSize + 2;
        Set<Coordinate> coordinates = new HashSet<>(128);

        for (int i = 0; i < mapSize - houseSizeWithMargin + 1; i++) {
            for (int j = 0; j < mapSize - houseSizeWithMargin + 1; j++) {
                boolean canBuild = true;
                canBuild = simCityPlan.getPassableSize(i, j) >= houseSizeWithMargin
                        && simCityPlan.getEmptySize(i + 1, j + 1) >= houseSize
                        && warMap.getDistanceToEnemy(i + 2, j + 2) >= MIN_DISTANCE_TO_ENEMY;
                if (canBuild) {
                    for (int k = 0; k < houseSize; k++) {
                        for (int l = 0; l < houseSize; l++) {
                            int canBuildX = i + 1 + k;
                            int canBuildY = j + 1 + l;
                            coordinates.add(new Coordinate(canBuildX, canBuildY));
                        }
                    }

                    boolean canBuildRangedBase = simCityPlan.getEmptySize(i, j) >= 5
                            && warMap.getDistanceToEnemy(i + 2, j + 2) >= MIN_DISTANCE_TO_ENEMY; // big building
                    if (canBuildRangedBase) {
                        for (int k = 0; k <= houseSize + 1; k++) { // hack
                            if (i - 1 >= 0) {
                                rangedBaseBuildCoordinates[i - 1][j + k] = new Coordinate(i, j);
                            }
                            if (j - 1 >= 0) {
                                rangedBaseBuildCoordinates[i + k][j - 1] = new Coordinate(i, j);
                            }
                            if (j + houseSize + 2 < mapSize) {
                                rangedBaseBuildCoordinates[i + k][j + houseSize + 2] = new Coordinate(i, j);
                            }
                            if (i + houseSize + 2 < mapSize) {
                                rangedBaseBuildCoordinates[i + houseSize + 2][j + k] = new Coordinate(i, j);
                            }
                        }
                    }

                    for (int k = 1; k <= houseSize; k++) {
                        houseBuildCoordinates[i + k][j + 0] = new Coordinate(i + 1, j + 1);
                        houseBuildCoordinates[i + 0][j + k] = new Coordinate(i + 1, j + 1);
                        houseBuildCoordinates[i + k][j + houseSize + 1] = new Coordinate(i + 1, j + 1);
                        houseBuildCoordinates[i + houseSize + 1][j + k] = new Coordinate(i + 1, j + 1);
/*
                        for (int l = 1; l <= houseSize; l++) {
                            if (houseBuildCoordinates[i + k][j + l] == null) {
                                houseBuildCoordinates[i + k][j + l] = new Coordinate(i + 1, j + 1);
                            }
                        }
*/
                    }
                }
            }
        }

/*
        for (int i = 0; i < mapSize; i++) {
            for (int j = 0; j < mapSize; j++) {
                if (DebugInterface.isDebugEnabled() && rangedBaseBuildCoordinates[i][j] != null) {
                    DebugInterface.print(rangedBaseBuildCoordinates[i][j].toString(), i, j);
                }
            }
        }
*/
    }

    public boolean isNeedBarracks() {
        return needBarracks;
    }

    public void setNeedBarracks(boolean needBarracks) {
        this.needBarracks = needBarracks;
    }

    public Coordinate getBuildCoordinates(model.Coordinate position) {
        return houseBuildCoordinates[position.getX()][position.getY()];
    }

    public Coordinate getRangedBaseBuildCoordinates(model.Coordinate position) {
        return rangedBaseBuildCoordinates[position.getX()][position.getY()];
    }
}
