package older.v11.mystrategy.strategies;

import model.*;
import older.v11.mystrategy.collections.AllEntities;
import older.v11.mystrategy.collections.SingleVisitCoordinateSet;
import older.v11.mystrategy.maps.EnemiesMap;
import older.v11.mystrategy.maps.EntitiesMap;
import older.v11.mystrategy.maps.RepairMap;
import older.v11.mystrategy.maps.SimCityMap;
import older.v11.mystrategy.maps.light.*;
import util.DebugInterface;
import util.StrategyDelegate;
import util.Task;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class DefaultStrategy implements StrategyDelegate {

    private EntitiesMap entitiesMap;
    private SimCityMap simCityMap;
    private RepairMap repairMap;
    private EnemiesMap enemiesMap;
    private int currentUnits;
    private int maxUnits;
    private AllEntities allEntities;
    private Player me;
    private PlayerView playerView;
    private DebugInterface debugInterface;
    //    private ResourcesMap resourceMap;
    private HarvestJobsMap harvestJobs;
    private boolean done;
    private boolean first;
    private boolean second;
    private boolean third;
    private BuildOrders buildOrders;
    private VisibilityMap visibility;
    private VirtualResources resources;
    private WarMap warMap;
    private WorkerJobsMap jobs;
    private SimCityPlan simCityPlan;

    public DefaultStrategy(
            BuildOrders buildOrders,
            VisibilityMap visibility,
            VirtualResources resources,
            WarMap warMap,
            SimCityPlan simCityPlan
    ) {
        this.buildOrders = buildOrders;
        this.visibility = visibility;
        this.resources = resources;
        this.warMap = warMap;
        this.simCityPlan = simCityPlan;
    }

    /**
     * attack -> build -> repair -> move
     */
    @Override
    public Action getAction(PlayerView playerView) {
        // определить фронт работ (добыча/постройка/починка/атака/расчистка/разведка/защита)
        // резервирование ресурсов
        // определить кто что делает сейчас // забрать работы
        // определить кто что может делать после пробежки
        // пометить ресурсы как добываемые
        // билд ордер с учётом доступных ресурсов (4 рабочих - барак - 5 рабочих - барак - ...) учитывая рост стоимости
        // определить где что строить
        // выгнать всех с потенциальных построек
        // определит кто куда идёт
        // рабочими идти только к новым патчам
        // (рабочие могу проходить сквозь друг-друга)
        // биться по правилам финала уже.
        // делеать милишников пачками по 5 штук
        // разбавлять лучников рабочими
        // сохранять стоимость юнитов низкой?
        // group units (attack/repair/build/harass/reconnaissance/harvest)
        // strategic orders
        // generate movements (attack/retreat/avoid active turrets/retreat to repair)
        // test movements

        // generate attacks+repairs

        this.playerView = playerView;
        allEntities = new AllEntities(playerView);


        entitiesMap = new EntitiesMap(playerView);
        me = Arrays.stream(playerView.getPlayers()).filter(player -> player.getId() == playerView.getMyId()).findAny().get();
        this.visibility.init(playerView, allEntities);
        this.resources.init(playerView, allEntities, entitiesMap);
        currentUnits = allEntities.getCurrentUnits();
        maxUnits = allEntities.getMaxUnits();
        enemiesMap = new EnemiesMap(playerView, entitiesMap, allEntities);
        this.warMap.init(playerView, entitiesMap, allEntities, enemiesMap, allEntities);
        buildOrders.init(playerView, allEntities);

        this.jobs = new WorkerJobsMap(
                playerView,
                entitiesMap,
                allEntities,
                enemiesMap,
                me,
                buildOrders,
                warMap,
                resources
        );

//        resourceMap = new ResourcesMap(playerView, entitiesMap, allEntities, enemiesMap, debugInterface);
        harvestJobs = new HarvestJobsMap(playerView, entitiesMap, allEntities, enemiesMap, me, resources, jobs);
        repairMap = new RepairMap(playerView, entitiesMap);
        simCityPlan.init(playerView, entitiesMap, allEntities, warMap, resources);
        simCityMap = new SimCityMap(playerView, entitiesMap, allEntities, warMap, simCityPlan);

        if (!second && allEntities.getMyBuildings().stream()
                .anyMatch(ent1 -> ent1.isMy(EntityType.RANGED_BASE) && !ent1.isActive())) {
            second = true;
            if (DebugInterface.isDebugEnabled()) {
//                System.out.println(playerView.getCurrentTick() + "BR");
            }
        }
        if (!third && allEntities.getMyBuildings().stream()
                .anyMatch(ent -> ent.isMy(EntityType.RANGED_BASE) && ent.isActive())) {
            third = true;
            if (DebugInterface.isDebugEnabled()) {
                System.out.println(playerView.getCurrentTick() + "BR+");
            }
        }

        // units
        // REAL WORK
        for (Entity unit : allEntities.getMyUnits()) {
            BuildAction buildAction = null;
            RepairAction repairAction = null;
            AttackAction attackAction = null;
            if (resources.getTotalResourceCount() > 0 && unit.getEntityType() == EntityType.BUILDER_UNIT) {
                if (unit.getTask() == Task.IDLE) { // assign idle workers to harvest or repair
                    Entity resource = harvestJobs.getResource(unit.getPosition());
                    Integer canBuildId = repairMap.canBuildId(unit.getPosition());
                    if (canBuildId != null) {
                        attackAction = null;
                        unit.setMoveAction(null); // bugfix this
                        repairAction = new RepairAction(canBuildId);
                    } else {
                        Integer canRepairId = repairMap.canRepairId(unit.getPosition());
                        if (canRepairId != null) {
                            repairAction = new RepairAction(canRepairId);
                        } else if (resource != null) {
                            attackAction = new AttackAction(resource.getId(), null);
                            resource.increaseDamage(unit.getProperties().getAttack().getDamage());
                        }
                    }
                    Coordinate buildCoordinates = simCityMap.getBuildCoordinates(unit.getPosition());
                    Coordinate rbBuildCoordinates = simCityMap.getRangedBaseBuildCoordinates(unit.getPosition());
                    if (simCityMap.isNeedBarracks() // hack
                            && me.getResource() >= playerView.getEntityProperties().get(EntityType.RANGED_BASE).getInitialCost()
                            && rbBuildCoordinates != null
                    ) {
                        attackAction = null;
                        unit.setMoveAction(null); // bugfix this
                        buildAction = new BuildAction(EntityType.RANGED_BASE, rbBuildCoordinates);
//                        buildOrders.placeBarracks(rbBuildCoordinates);
//                    maxUnits += playerView.getEntityProperties().get(EntityType.RANGED_BASE).getPopulationProvide();

                        simCityMap.setNeedBarracks(false);
                    }
                    if (needMoreHouses()
                            && buildOrders.isFreeToAdd()
                            && me.getResource() >= playerView.getEntityProperties().get(EntityType.HOUSE).getInitialCost()
                            && buildCoordinates != null
                    ) {
                        attackAction = null;
                        unit.setMoveAction(null); // bugfix this
                        buildAction = new BuildAction(EntityType.HOUSE, buildCoordinates);
                        maxUnits += playerView.getEntityProperties().get(EntityType.HOUSE).getPopulationProvide();
                    }
                } else if (unit.getTask() == Task.BUILD) {
//                    DebugInterface.print("B", unit.getPosition());
                    Entity order = buildOrders.getOrder(unit.getPosition());
                    if (order != null) {
                        Entity entity = entitiesMap.getEntity(order.getPosition());
                        if (entity.getEntityType() == order.getEntityType()) {
                            repairAction = new RepairAction(entity.getId());
                        } else {
                            buildAction = new BuildAction(order.getEntityType(), order.getPosition());
                        }
                    }
                }


                unit.setAttackAction(attackAction);
                unit.setBuildAction(buildAction);
                unit.setRepairAction(repairAction);
            } else {

                if (unit.getEntityType() == EntityType.RANGED_UNIT) {
                    // RANGER ATTACK
                    EntityType[] validAutoAttackTargets;
                    validAutoAttackTargets = new EntityType[0];
                    Entity enemy = entitiesMap.choosePossibleAttackTarget(unit);
                    if (enemy != null) {
                        attackAction = new AttackAction(enemy.getId(), null);
                        enemy.increaseDamage(unit.getProperties().getAttack().getDamage());
                    }
                    unit.setAttackAction(attackAction);
                    unit.setBuildAction(null);
                    unit.setRepairAction(null);
                } else {
                    // MELEE+WORKERS
                    EntityType[] validAutoAttackTargets;
                    validAutoAttackTargets = new EntityType[0];
                    attackAction = new AttackAction(
                            null, new AutoAttack(5, validAutoAttackTargets)
                    );
                    unit.setAttackAction(attackAction);
                    unit.setBuildAction(null);
                    unit.setRepairAction(null);
                }
            }
        }

        // JUST MOVING
        // run first
        allEntities.getMyWorkers().stream().filter(worker -> worker.getTask() == Task.RUN_FOOLS).forEach(unit -> {
            harvestJobs.decideMoveForBuilderUnit(unit);
        });
        for (Entity unit : allEntities.getMyUnits()) {
            MoveAction moveAction;
            if (resources.getTotalResourceCount() > 0 && unit.getEntityType() == EntityType.BUILDER_UNIT) {
                harvestJobs.decideMoveForBuilderUnit(unit);
            } else { // all
                if (unit.getEntityType() == EntityType.RANGED_UNIT) {
                    continue;
                    //skip
                } else {
                    Coordinate moveTo = warMap.getPositionClosestToEnemy(unit);
                    if (moveTo == null || Objects.equals(moveTo, unit.getPosition())) { // hack
                        if (playerView.isOneOnOne()) {
                            moveTo = new Coordinate(72, 72);
                        } else {
                            moveTo = new Coordinate(7, 72);
                        }
                    }
                    moveAction = new MoveAction(moveTo, true, true);
                    unit.setMoveAction(moveAction);
                }
            }
        }
        harvestJobs.printTakenMap();

        warMap.updateFreeSpaceMaskForRangedUnits();
        for (Entity unit : allEntities.getMyUnits()) {
            if (unit.getEntityType() == EntityType.RANGED_UNIT) {
                // MOVE RANGER
                warMap.decideMoveForRangedUnit(unit);
            }
        }

/*
        for (Entity unit : allEntities.getMyUnits()) {
            if (unit.getAttackAction() != null) {
                if (unit.getAttackAction().getAutoAttack() != null) {
                    DebugInterface.println("U", unit.getPosition(), 2);
                } else {
                    DebugInterface.println("A", unit.getPosition(), 2);
                }
            } else if (unit.getBuildAction() != null) {
                DebugInterface.println("B", unit.getPosition(), 2);
            } else if (unit.getRepairAction() != null) {
                DebugInterface.println("T", unit.getPosition(), 2);
            } else if (unit.getMoveAction() != null) {
                if (unit.getTask() == Task.RUN_FOOLS) {
                    DebugInterface.println("R", unit.getPosition(), 2);
                } else {
                    DebugInterface.println("M", unit.getPosition(), 2);
                }
            }
            if (unit.getMoveAction() != null) {
                DebugInterface.line(unit.getPosition(), unit.getMoveAction().getTarget());
            }
        }
*/

        // buildings
        handleBuildings();


        Action result = new Action(new java.util.HashMap<>());
        for (Entity actor : allEntities.getMyActors()) {
            if (actor.hasAction()) {
                result.getEntityActions().put(actor.getId(), new EntityAction(
                        actor.getMoveAction(),
                        actor.getBuildAction(),
                        actor.getAttackAction(),
                        actor.getRepairAction()
                ));
            }
        }

        return result;
    }

    private boolean needMoreHouses() {
        return maxUnits == 0 || (maxUnits - (currentUnits + me.getResource() / (maxUnits <= 150 ? 10 : 50))) * 100 / maxUnits < 20;
    }

    private void handleBuildings() {
        for (Entity building : allEntities.getMyBuildings()) {
            BuildAction buildAction = null;
            if (building.getProperties().getBuild() != null) {
                buildAction = getBuildingAction(playerView, building, building.getProperties(), null);
            }
            building.setMoveAction(null);
            building.setAttackAction(new AttackAction(
                    null, new AutoAttack(building.getProperties().getSightRange(), new EntityType[0])
            ));
            building.setRepairAction(null);
            building.setBuildAction(buildAction);
        }
    }

    private BuildAction getBuildingAction(PlayerView playerView, Entity entity, EntityProperties properties, BuildAction buildAction) {
        EntityType entityType = properties.getBuild().getOptions()[0];
        if (entityType != EntityType.BUILDER_UNIT && playerView.getCurrentTick() < 20) {
            return buildAction;
        }

/*
        if (DebugInterface.isDebugEnabled()) {
            System.out.println(playerView.getCurrentTick() + ":" + allEntities.getMyBuilders().size());
        }

*/

/*
failedLimits
        int buildersLimit = allEntities.getEnemyBuilders().size() + 20;
        if (playerView.isRound2()) {
            if (maxUnits > 30*0.7) {
                buildersLimit = Math.max((int) (maxUnits * 0.7), buildersLimit);
            }
        }

        if (playerView.isFinials()) {
            buildersLimit = 110;
        }
*/
        int buildersLimit = allEntities.getEnemyWorkers().size() + 20;

        if (playerView.isFinials()) {
            buildersLimit = 40 + (allEntities.getEnemyUnits().stream().noneMatch(enemy -> Math.min(enemy.getPosition().getY(), enemy.getPosition().getX()) < 40) ? 40 : 0);
        } else if (playerView.isRound2()) {
            buildersLimit = 40 + (allEntities.getEnemyUnits().stream().noneMatch(enemy -> Math.min(enemy.getPosition().getY(), enemy.getPosition().getX()) < 40) ? 40 : 0);
        } else {
            buildersLimit = 61;
        }


        if (entityType == EntityType.BUILDER_UNIT) {
            if (allEntities.getMyWorkers().size() < buildersLimit) {
                SingleVisitCoordinateSet testStand = new SingleVisitCoordinateSet();
                testStand.addAll(entity.getAdjacentCoordinates());
                int resourceSpotCount = 0;
                int iterationsRemaining = 40;
                while (!testStand.isEmpty() && iterationsRemaining > 0) {
                    iterationsRemaining--;
                    for (Coordinate pos : testStand) {
                        boolean isResource = false;
                        for (Coordinate adjacent : pos.getAdjacentList()) {
                            boolean resource = resources.getResourceCount(adjacent) > 0;
                            if (!resource && entitiesMap.isPassable(adjacent)) {
                                testStand.addOnNextStep(adjacent);
                            } else if (resource) {
                                isResource = true;
                            }
                        }
                        if (isResource) {
                            resourceSpotCount++;
//                            DebugInterface.println("Rx" + resourceSpotCount, pos, 0);
                        }/* else {
                            DebugInterface.println("X", pos, 1);
                        }*/
                    }
                    testStand.nextStep();
                }
                if (iterationsRemaining > 0) {
                    buildersLimit = Math.min(buildersLimit, resourceSpotCount);
                }

                if (allEntities.getMyWorkers().size() >= buildersLimit) {
                    return buildAction;
                }
            } else {
                return buildAction;
            }
        }

        Coordinate defaultBuildPosition = new Coordinate(
                entity.getPosition().getX() + properties.getSize(),
                entity.getPosition().getY() + properties.getSize() - 1
        );
        if (entityType == EntityType.BUILDER_UNIT) {
            Coordinate buildPosition = defaultBuildPosition;
            List<Coordinate> adjacentFreePoints = entity.getAdjacentCoordinates();
            adjacentFreePoints = adjacentFreePoints.stream()
                    .filter(point -> !point.isOutOfBounds())
                    .filter(point -> entitiesMap.isEmpty(point))
                    .collect(Collectors.toList());

            // get any free point
            Optional<Coordinate> any = adjacentFreePoints.stream().findAny();
            if (any.isPresent()) {
                buildPosition = any.get();
            }

            buildPosition = harvestJobs.getPositionClosestToResource(buildPosition, adjacentFreePoints);
            buildAction = new BuildAction(
                    EntityType.BUILDER_UNIT,
                    buildPosition
            );
        } else {
            if (entityType == EntityType.MELEE_UNIT && allEntities.getMyMeleeUnits().size() > 5) {
                return buildAction;
            }
            Coordinate buildPosition = defaultBuildPosition;
            List<Coordinate> adjacentFreePoints = entity.getAdjacentCoordinates();
            adjacentFreePoints = adjacentFreePoints.stream()
                    .filter(point -> !point.isOutOfBounds())
                    .filter(point -> entitiesMap.isEmpty(point))
                    .collect(Collectors.toList());

            // get any free point
            Optional<Coordinate> any = adjacentFreePoints.stream().findAny();
            if (any.isPresent()) {
                buildPosition = any.get();
            }
            buildPosition = warMap.getPositionClosestToEnemy(buildPosition, adjacentFreePoints);
            buildAction = new BuildAction(
                    entityType,
                    buildPosition
            );
        }
        return buildAction;
    }

    @Override
    public boolean isDone() {
        return done;
    }

    @Override
    public StrategyDelegate getNextStage() {
        return null;
    }
}