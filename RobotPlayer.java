package team166;

import battlecode.common.*;

import java.util.*;

public class RobotPlayer {
    static RobotController rc;
    static Team myTeam;
    static MapLocation myHQ;
    static MapLocation enemyHQ;
    static Team enemyTeam;
    static MapLocation rally;
    static int myRange;
    static int SENSE_RANGE = 40;
    static double LOW_HEALTH = 0.20;
    static double currentForce;
    static Direction myDirection;
    static boolean clockwise;
    static boolean bug;
    static int howClose;
    static int[][] surrounding;
    static int minOre;
    static int numTowers;
    static Random rand;
    static RobotInfo[] myRobots;
    static boolean attack;
    static int minimunThreat = 10;
    static int minimunForce = 25;
    static int statusChannel;
    static int surroundingEnemyForce;
    static MapLocation target;

    static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST,
            Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST,
            Direction.WEST, Direction.NORTH_WEST};

    public static void run(RobotController tomatojuice) {
        rc = tomatojuice;
        rand = new Random(rc.getID());
        attack = false;
        myRange = rc.getType().attackRadiusSquared;
        MapLocation enemyLoc = rc.senseEnemyHQLocation();
        Direction lastDirection = null;
        myTeam = rc.getTeam();
        enemyTeam = myTeam.opponent();
        myHQ = rc.senseHQLocation();
        enemyHQ = rc.senseEnemyHQLocation();
        rally = getRally();
        howClose = 100000;
        bug = false;
        clockwise = false;
        surrounding = new int[3][3];
        minOre = -1;
        statusChannel = -1;

        rc.setIndicatorString(0, "I am a " + rc.getType());

        while (true) {
            try {
                switch (rc.getType()) {
                    case HQ:
                        hq();
                        break;
                    case TOWER:
                        tower();
                        break;
                    case BASHER:
                        basher();
                        break;
                    case SOLDIER:
                        soldier();
                        break;
                    case TANK:
                        tank();
                        break;
                    case BEAVER:
                        beaver();
                        break;
                    case BARRACKS:
                        barracks();
                        break;
                    case TANKFACTORY:
                        tankFactory();
                        break;
                    case MINERFACTORY:
                        minerFactory();
                        break;
                    case MINER:
                        miner();
                        break;
                    case SUPPLYDEPOT:
                        break;
                    case HANDWASHSTATION:
                        break;
                    default:
                        System.out.println("... I don't know... help");
                }
            } catch (Exception e) {
                System.out.println(rc.getType() + "  Exception:");
                e.printStackTrace();
            }
            rc.yield();
        }
    }

    /**
     * ******Robot Type Methods********
     */
    static void hq() throws GameActionException {
        myRobots = rc.senseNearbyRobots(999999, myTeam);
        int numSoldiers = 0;
        int numBashers = 0;
        int numBeavers = 0;
        int numBarracks = 0;
        int numSupplyDepots = 0;
        int numTankFactories = 0;
        int numMinerFactories = 0;
        int numMiners = 0;
        int numTanks = 0;
        //get & broadcast our numbers
        for (RobotInfo r : myRobots) {
            switch (r.type) {
                case BEAVER:
                    numBeavers++;
                    break;
                case SOLDIER:
                    numSoldiers++;
                    break;
                case BASHER:
                    numBashers++;
                    break;
                case MINER:
                    numMiners++;
                    break;
                case BARRACKS:
                    numBarracks++;
                    break;
                case SUPPLYDEPOT:
                    numSupplyDepots++;
                    break;
                case TANKFACTORY:
                    numTankFactories++;
                    break;
                case MINERFACTORY:
                    numMinerFactories++;
                    break;
            }
        }
        rc.broadcast(0, numBeavers);
        rc.broadcast(1, numSoldiers);
        rc.broadcast(2, numBashers);
        rc.broadcast(3, numTanks);
        rc.broadcast(4, numMiners);
        rc.broadcast(10, numSoldiers + numBashers + numTanks); //total army size
        rc.broadcast(99, numSupplyDepots);
        rc.broadcast(100, numBarracks);
        rc.broadcast(101, numTankFactories);
        rc.broadcast(102, numMinerFactories);

        if (rc.isWeaponReady()) {
            towerAttack();
        }
        rc.broadcast(300, surroundingEnemyForce);

        if (rc.isCoreReady() && rc.getTeamOre() >= 100 && numBeavers < 2) {
            trySpawn(rc.getLocation().directionTo(enemyHQ), RobotType.BEAVER);
        }
        shareSupplies();
        if (rc.readBroadcast(53) == 1) {
            if (target != findClosestEnemyTower()) {
                rc.broadcast(53, 0);
            }
        }
        if (rc.readBroadcast(301) == 0) {
            //we must assign channels
            //300 - HQ status
            //301 & 302 - Tower 1 location (Closest to HQ)
            //303 - Tower 1 status
            //304 & 305 - Tower 2 location
            //306 - Tower 2 status
            //307 & 308 - Tower 3 location 
            //309 - Tower 3 status
            //310 & 311 - Tower 4 location
            //312 - Tower 4 status
            //313 & 314 - Tower 5 location
            //315 - Tower 5 status
            //316 & 317 - Tower 6 location (Furthest from HQ)
            //318 - Tower 6 status
            rc.setIndicatorString(1, "Assigning Channels");
            MapLocation[] towers = rc.senseTowerLocations();
            numTowers = towers.length;
            int closest;
            for (int i = 0; i < numTowers; i++) {
                closest = i;
                for (int j = i + 1; j < numTowers; j++) {
                    if (towers[closest].distanceSquaredTo(myHQ) > towers[j].distanceSquaredTo(myHQ)) {
                        closest = j;
                    }
                }
                if (closest != i) {
                    MapLocation temp = towers[i];
                    towers[i] = towers[closest];
                    towers[closest] = temp;
                }
            }
            //towers should be sorted with closest first now
            int channel = 301;
            for (int i = 0; i < towers.length; i++) {
                rc.broadcast(channel, towers[i].x);
                rc.broadcast(channel + 1, towers[i].y);
                channel += 3;
            }
        } else {
            if (numTowers != rc.senseTowerLocations().length) { //somebody died! make sure no more robots try to defend them
                MapLocation cur;
                for (int i = 301; i <= 316; i += 3) {
                    cur = new MapLocation(rc.readBroadcast(i), rc.readBroadcast(i + 1));
                    if (!(rc.canSenseLocation(cur) && rc.senseRobotAtLocation(cur) != null
                            && rc.senseRobotAtLocation(cur).type == RobotType.TOWER)) {
                        rc.broadcast(i + 2, -1);
                    }
                }
                numTowers = rc.senseTowerLocations().length;
            } else {
                MapLocation distress = checkStatuses();
                if (distress != enemyHQ) {
                    rc.setIndicatorString(1, "Distress found at: " + distress + " @ " + Clock.getRoundNum());
                    broadcastLocation(distress);
                } else if (getArmyThreat(rc.senseNearbyRobots(rally, 20, myTeam)) > minimunForce) { //some minimum
                    target = findClosestEnemyTower();
                    broadcastLocation(target);
                    rc.broadcast(53, 1);
                } else {
                    rc.setIndicatorString(1, "Sending to rally @ " + Clock.getRoundNum());
                    broadcastLocation(rally);
                }
            }

        }
    }

    static void tower() throws GameActionException {
        if (statusChannel == -1) {
            statusChannel = getTowerChannel(rc.getLocation());
            //rc.setIndicatorString(1, "Channel is: "+statusChannel);
        }
        if (rc.isWeaponReady()) {
            towerAttack();
        }
        shareSupplies();
        rc.broadcast(statusChannel, surroundingEnemyForce);
    }

    static void basher() throws GameActionException {
        RobotInfo[] adjacentEnemies = rc.senseNearbyRobots(2, enemyTeam);

        if (rc.isCoreReady()) {
            moveToRally();
        }
        if (rc.getHealth() <= rc.getType().maxHealth * LOW_HEALTH) {
            dumpSupply();
        } else {
            shareSupplies();
        }
    }

    static void soldier() throws GameActionException {
        if (rc.isWeaponReady()) {
            attackSomething();
        }
        if (rc.isCoreReady()) {
            if (retreat()) {
                tryMove(rc.getLocation().directionTo(myHQ), "Retreat");
            } else {
                moveToRally();
            }
        }
        if (rc.getHealth() <= rc.getType().maxHealth * LOW_HEALTH) {
            dumpSupply();
        } else {
            shareSupplies();
        }
    }

    static void tank() throws GameActionException {
        if (rc.isWeaponReady()) {
            attackSomething();
        }
        if (rc.isCoreReady()) {
            if (retreat()) {
                howClose = 10000;
                bug(myHQ);
                //TODO: retreat to nearest tower
            } else {
                moveToRally();
            }
        }
        if (rc.getHealth() <= rc.getType().maxHealth * LOW_HEALTH) {
            dumpSupply();
        } else {
            shareSupplies();
        }
    }

    static void miner() throws GameActionException {
        if (minOre == -1) {
            minOre = (int) rc.senseOre(rc.getLocation()) / 3;
            if (minOre < 1) minOre = 1;
            else if (minOre > 5) minOre = 5;
        }
        if (rc.isWeaponReady()) {
            attackSomething();
        }
        if (rc.isCoreReady()) {
            RobotInfo[] enemies = rc.senseNearbyRobots(SENSE_RANGE, enemyTeam);
            if (enemies.length > 0) {
                tryMove(rc.getLocation().directionTo(enemies[0].location).opposite(), "Run away");
            } else if (rc.canMine() && rc.senseOre(rc.getLocation()) > minOre) {
                rc.setIndicatorString(1, "Mining...");
                rc.mine();
            } else {
                MapLocation goal = findBestOre();
                if (goal != rc.getLocation()) {
                    rc.setIndicatorString(1, "Moving to sensed square");
                    bug(goal);
                } else if (rc.readBroadcast(200) >= minOre && rc.readBroadcast(201) != 0 && rc.readBroadcast(202) != 0) {
                    rc.setIndicatorString(1, "Moving to broadcasted square: " + rc.readBroadcast(201) + " " + rc.readBroadcast(202));
                    bug(new MapLocation(rc.readBroadcast(201), rc.readBroadcast(202)));
                } else {
                    rc.setIndicatorString(1, "Moving in random direction");
                }
            }
        }
        if (rc.getHealth() <= rc.getType().maxHealth * LOW_HEALTH) {
            dumpSupply();
        } else {
            shareSupplies();
        }
    }

    static void beaver() throws GameActionException {
        if (rc.isWeaponReady()) {
            attackSomething();
        }

        if (rc.isCoreReady()) {
            if (Clock.getRoundNum() >= rc.getRoundLimit() - 250 && isTied() && rc.getTeamOre() >= 200) {
                tryBuild(directions[rand.nextInt(8)], RobotType.HANDWASHSTATION);
            } else {
                int numMinerFactory, numBarracks, numTankFactory, numSupplyDepots, armySize, ore;
                ore = (int) rc.getTeamOre();
                numMinerFactory = rc.readBroadcast(102);
                numBarracks = rc.readBroadcast(100);
                numTankFactory = rc.readBroadcast(101);
                numSupplyDepots = rc.readBroadcast(99);
                armySize = rc.readBroadcast(10);
                rc.setIndicatorString(1, "trying to build");
                if (ore >= 500 && numMinerFactory < 1) {
                    tryBuild(directions[rand.nextInt(8)], RobotType.MINERFACTORY);
                } else if (ore >= 400 && numMinerFactory > 0 && (numBarracks < 1 || (numTankFactory > 0 && numBarracks < numTankFactory - 2))) { //TODO: adjust to make more if we can't produce units fast enough
                    tryBuild(directions[rand.nextInt(8)], RobotType.BARRACKS);
                } else if (ore >= 500 && numBarracks > 0 && (numTankFactory < 0 || ore > 900) && numMinerFactory > 0) {
                    tryBuild(rc.getLocation().directionTo(myHQ), RobotType.TANKFACTORY);
                } else if (ore >= 100 && numSupplyDepots < 3 && armySize > 25 + 50 * (numSupplyDepots)) {
                    tryBuild(rc.getLocation().directionTo(myHQ), RobotType.SUPPLYDEPOT);
                } else { //move and mine
                    if (rc.getLocation().distanceSquaredTo(myHQ) < 4) {
                        rc.setIndicatorString(1, "trying to move away");
                        tryMove(rc.getLocation().directionTo(myHQ).opposite(), "");
                    } else if (rc.getLocation().distanceSquaredTo(myHQ) > 16) {
                        rc.setIndicatorString(1, "trying to move to HQ");
                        tryMove(rc.getLocation().directionTo(myHQ), "");
                    } else if (rc.senseOre(rc.getLocation()) > 1) {
                        rc.setIndicatorString(1, "mining");
                        rc.mine();
                    } else {
                        rc.setIndicatorString(1, "trying to move randomly");
                        tryMove(directions[rand.nextInt(8)], "");
                    }
                }
            }
        }
        if (rc.getHealth() <= rc.getType().maxHealth * LOW_HEALTH) {
            dumpSupply();
        } else {
            shareSupplies();
        }
    }

    static void barracks() throws GameActionException {
        int fate = rand.nextInt(10000);
        // get information broadcasted by the HQ
        int numBeavers = rc.readBroadcast(0);
        int numSoldiers = rc.readBroadcast(1);
        int numBashers = rc.readBroadcast(2);
        int numTanks = rc.readBroadcast(3);
        int numTankFactories = rc.readBroadcast(101);
        if (rc.isCoreReady() && rc.getTeamOre() >= 80) {
            if (rc.getTeamOre() > 80 && numTankFactories == 0) {//&& fate % 2 == 0
                trySpawn(rc.getLocation().directionTo(enemyHQ), RobotType.SOLDIER);
            } else if ((rc.isCoreReady() && rc.getTeamOre() > 80 && numTanks >= numSoldiers) || (rc.isCoreReady() && rc.getTeamOre() > 500)) {
                trySpawn(rc.getLocation().directionTo(enemyHQ), RobotType.SOLDIER);
            }
        }
        shareSupplies();
    }

    static void tankFactory() throws GameActionException {
        int numTanks = rc.readBroadcast(3);
        if ((rc.isCoreReady() && rc.getTeamOre() >= 250) || (rc.isCoreReady() && rc.getTeamOre() > 1000)) {
            trySpawn(rc.getLocation().directionTo(enemyHQ), RobotType.TANK);
        }
        shareSupplies();
    }

    static void minerFactory() throws GameActionException {
        if (rc.isCoreReady() && rc.getTeamOre() >= 50 && rc.readBroadcast(4) < 25) { // TODO make function of map size
            trySpawn(rc.getLocation().directionTo(enemyHQ), RobotType.MINER);
        }
        shareSupplies();
    }

    /**
     * ******Attack, Movement, and Creation Methods********
     */
    // This method will attack an enemy in sight, if there is one
    static void towerAttack() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(myRange + 10, enemyTeam);
        double force;
        if (enemies.length > 0) {
            RobotInfo target = enemies[0];
            int targetThreat = getThreatLevel(target.type);
            force = getDamage(target.type);
            for (int i = 1; i < enemies.length; i++) {
                force += getDamage(enemies[i].type);
                if (enemies[i].location.distanceSquaredTo(rc.getLocation()) <= myRange) {
                    if (getThreatLevel(enemies[i].type) > targetThreat) {
                        target = enemies[i];
                        targetThreat = getThreatLevel(enemies[i].type);

                    } else if (getThreatLevel(enemies[i].type) == targetThreat) {
                        if ((enemies[i].location.distanceSquaredTo(rc.getLocation()) <
                                target.location.distanceSquaredTo(rc.getLocation())) ||
                                (enemies[i].health < target.health)) {
                            target = enemies[i];
                        }
                    }
                }
            }
            if (rc.getLocation().distanceSquaredTo(target.location) <= myRange) {
                rc.attackLocation(target.location);
            }
            surroundingEnemyForce = (int) force;
        } else {
            surroundingEnemyForce = 0;
        }
    }

    static void attackSomething() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
        if (enemies.length > 0) {
            RobotInfo target = enemies[0];
            int targetThreat = getThreatLevel(target.type);
            for (int i = 1; i < enemies.length; i++) {
                if (getThreatLevel(enemies[i].type) > targetThreat) {
                    target = enemies[i];
                    targetThreat = getThreatLevel(enemies[i].type);
                } else if (getThreatLevel(enemies[i].type) == targetThreat) {
                    if ((enemies[i].location.distanceSquaredTo(rc.getLocation()) <
                            target.location.distanceSquaredTo(rc.getLocation())) ||
                            (enemies[i].health < target.health)) {
                        target = enemies[i];
                    }
                }
            }
            rc.attackLocation(target.location);
        }
    }

    static double getDamage(RobotType r) {
        switch (r) {
            case BEAVER:
                return 2;
            case MINER:
                return 1.5;
            case COMPUTER:
                return 0;
            case BASHER:
                return 4;
            case COMMANDER:
                return 10;
            case LAUNCHER:
                return 5; //for being able to fire missiles, it needs to be a higher priority target than soldiers & bashers
            case MISSILE:
                return 20;
            case TOWER:
                return 10;
            case SOLDIER:
                return 4;
            case TANK:
                return 6.7;
            case DRONE:
                return 2.7;
            case HQ:
                return 10 + 2 * (rc.senseEnemyTowerLocations().length); //micheal to update?
            default: //building probably
                return 0;
        }
    }

    static int getThreatLevel(RobotType r) {
        switch (r) {
            case BEAVER:
                return 2;
            case MINER:
                return 3;
            case COMPUTER:
                return 1;
            case BASHER:
                return 6;
            case COMMANDER:
                return 9;
            case LAUNCHER:
                return 8;
            case MISSILE:
                return 11; //temp
            case TOWER:
                return 10;
            case HQ:
                return 10;
            case SOLDIER:
                return 4;
            case TANK:
                return 7;
            case DRONE:
                return 5;
            default: //building probably
                return 0;
        }
    }

    static double getArmyThreat(RobotInfo[] robots) {
        double result = 0.0;
        for (int i = 0; i < robots.length; i++) {
            result += getDamage(robots[i].type);
        }
        return result;
    }

    static boolean canWeWin(MapLocation area) {
        double us = getArmyThreat(rc.senseNearbyRobots(area, 20, myTeam));
        double them = getArmyThreat(rc.senseNearbyRobots(area, 20, enemyTeam));

        return us > them;
    }

    static boolean retreat() {
//        RobotInfo [] enemyRobots = rc.senseNearbyRobots(SENSE_RANGE, enemyTeam);
//        if (enemyRobots.length>0){
//            double us = getArmyThreat(rc.senseNearbyRobots(SENSE_RANGE, myTeam));
//            double them = getArmyThreat(enemyRobots);
//            currentForce = us;
//            return us < them;
//        }
        return false;
    }

    // This method will attempt to move in Direction d (or as close to it as possible)
    static boolean tryMove(Direction d, String info) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = {0, 1, -1, 2, -2};
        int dirint = directionToInt(d);
        boolean blocked = false;
        while (offsetIndex < 5 && !moveIsGood(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
            offsetIndex++;
        }
        if (offsetIndex < 5) {
            if (rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
                rc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
                mapMove(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
                //rc.setIndicatorString(1, info + "moved");
            }
            return true;
        } else {
            //rc.setIndicatorString(1, info + "move failed");
            return false;
        }
    }

    static void moveToRally() throws GameActionException {
        int distanceToGoal = rc.getLocation().distanceSquaredTo(rally);
        //rc.setIndicatorString(1, "force: "+currentForce);
//        if (attack || (distanceToGoal < rc.getLocation().distanceSquaredTo(myHQ) && Clock.getRoundNum() > 260 && Clock.getRoundNum() % 250 < 10)) {
//            rally = new MapLocation(rc.readBroadcast(50), rc.readBroadcast(51));
//            attack = true;
//            howClose = rally.distanceSquaredTo(rc.getLocation());
//        }
        MapLocation broadcasted = getBroadcastedRally();
        if (rally != broadcasted) {
            rally = broadcasted;
            howClose = rally.distanceSquaredTo(rc.getLocation());
        }

        if (distanceToGoal > 4)
            bug(rally);
        else
            rc.setIndicatorString(1, "waiting...");
        //TODO: while waiting, find nearish enemies and KILL THEM
    }

    static void bug(MapLocation goal) throws GameActionException {
        //get straight line to goal
        //if: you can move straight to the goal && (it gets you closer than ever before || to the left gets you closer || to the right gets you closer)
        //try to move that way
        //else if: the block in front of you is null
        //pick clockwise/counterclockwise somehow
        //rotate in that direction until you have a square that is not null immediatly before a null one
        //if you don't find anything valid in that direction, try rotating the other way
        //if you find something valid, try to move there
        //else: just move as close as you can to goal directon, you are probably behind somebody
        //are you closer? then save that distance
        String digest = "";
        Direction optimal = rc.getLocation().directionTo(goal);
        Direction closeEnough = dirCloserToRally(howClose, optimal);
        //rc.setIndicatorString(2, "dirCloserToRally")
        if (rc.canMove(closeEnough) && closeEnough != Direction.NONE) {
            digest += "going straight there:";
            tryMove(closeEnough, digest);
            bug = false;
        } else if (bug || terrainTileIsNull(optimal)) {
            digest += "bug:";
            bug = true;
            Direction bugDir = rotate(optimal);
            if (bugDir == Direction.NONE) {
                digest += " switch to: " + !clockwise;
                clockwise = !clockwise;
                bugDir = rotate(optimal);

            }
            if (bugDir != Direction.NONE && moveIsGood(bugDir)) {
                digest += " found: " + bugDir;
                tryMove(bugDir, digest);
                mapMove(bugDir);
            }
        } else {
            digest += "movearound: ";
            tryMove(optimal, digest);
        }

        if (rally.distanceSquaredTo(rc.getLocation()) < howClose) {
            howClose = rally.distanceSquaredTo(rc.getLocation());
        }
    }

    static Direction rotate(Direction cur) {
        int i = 0;
        if (clockwise) {
            while ((terrainTileIsNull(cur) || !terrainTileIsNull(cur.rotateRight()) || wasThereBefore(cur)) && i < 8) {
                cur = cur.rotateRight();
                i++;
            }
        } else {
            while ((terrainTileIsNull(cur) || !terrainTileIsNull(cur.rotateLeft()) || wasThereBefore(cur)) && i < 8) {
                cur = cur.rotateLeft();
                i++;
            }
        }
        if (i == 8 && (terrainTileIsNull(cur) || !terrainTileIsNull(cur.rotateLeft()) || wasThereBefore(cur))) {
            return Direction.NONE;
        }

        return cur;
    }

    // This method will attempt to spawn in the given direction (or as close to it as possible)
    static void trySpawn(Direction d, RobotType type) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
        int dirint = directionToInt(d);
        boolean blocked = false;
        while (offsetIndex < 8 && !rc.canSpawn(directions[(dirint + offsets[offsetIndex] + 8) % 8], type)) {
            offsetIndex++;
        }
        if (offsetIndex < 8) {
            rc.spawn(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
        } else {
            rc.setIndicatorString(1, "I can't spawn things");
        }
    }

    // This method will attempt to build in the given direction (or as close to it as possible)
    static boolean tryBuild(Direction d, RobotType type) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
        int dirint = directionToInt(d);
        boolean blocked = false;
        while (offsetIndex < 8 && (!rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])
                || !mapLocationIsOnGrid(rc.getLocation().add(directions[(dirint + offsets[offsetIndex] + 8) % 8])))) {
            offsetIndex++;
        }
        if (offsetIndex < 8) {
            rc.build(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
            return true;
        } else {
            rc.setIndicatorString(1, "could not build");
            return false;
        }
    }

    /**
     * ****** Directions and Find ********
     */
    static int directionToInt(Direction d) {
        switch (d) {
            case NORTH:
                return 0;
            case NORTH_EAST:
                return 1;
            case EAST:
                return 2;
            case SOUTH_EAST:
                return 3;
            case SOUTH:
                return 4;
            case SOUTH_WEST:
                return 5;
            case WEST:
                return 6;
            case NORTH_WEST:
                return 7;
            default:
                return -1;
        }
    }

    static MapLocation getRally() {
        return new MapLocation((enemyHQ.x + myHQ.x) / 2,
                (enemyHQ.y + myHQ.y) / 2);

    }

    static MapLocation findClosestEnemyTower() {
        MapLocation[] towers = rc.senseEnemyTowerLocations();
        MapLocation closest = enemyHQ;
        int min_distance = rally.distanceSquaredTo(enemyHQ);
        int cur;

        for (int i = 0; i < towers.length; i++) {
            cur = rally.distanceSquaredTo(towers[i]);
            if (cur < min_distance) {
                closest = towers[i];
                min_distance = cur;
            }
        }
        return closest;


    }

    /**
     * ****** Movement Helpers ********
     */
    static boolean wasThereBefore(Direction move) {
        int result = -1;
        switch (move) {
            case NORTH:
                result = surrounding[0][1];
                break;
            case NORTH_EAST:
                result = surrounding[0][2];
                break;
            case EAST:
                result = surrounding[1][2];
                break;
            case SOUTH_EAST:
                result = surrounding[2][2];
                break;
            case SOUTH:
                result = surrounding[2][1];
                break;
            case SOUTH_WEST:
                result = surrounding[2][0];
                break;
            case WEST:
                result = surrounding[1][0];
                break;
            case NORTH_WEST:
                result = surrounding[0][0];
                break;
        }
        return result == 1;
    }

    static void mapMove(Direction move) {
        surrounding = new int[3][3];
        switch (move.opposite()) {
            case NORTH:
                surrounding[0][1] = 1;
                break;
            case NORTH_EAST:
                surrounding[0][2] = 1;
                break;
            case EAST:
                surrounding[1][2] = 1;
                break;
            case SOUTH_EAST:
                surrounding[2][2] = 1;
                break;
            case SOUTH:
                surrounding[2][1] = 1;
                break;
            case SOUTH_WEST:
                surrounding[2][0] = 1;
                break;
            case WEST:
                surrounding[1][0] = 1;
                break;
            case NORTH_WEST:
                surrounding[0][0] = 1;
                break;
        }
    }

    static Direction dirCloserToRally(int closest, Direction goal) {
        if (closest > rc.getLocation().add(goal).distanceSquaredTo(rally) && !terrainTileIsNull(goal) && !wasThereBefore(goal)) {
            return goal;
        } else if (closest > rc.getLocation().add(goal.rotateLeft()).distanceSquaredTo(rally) && !terrainTileIsNull(goal.rotateLeft()) && !wasThereBefore(goal.rotateLeft())) {
            return goal.rotateLeft();
        } else if (closest > rc.getLocation().add(goal.rotateRight()).distanceSquaredTo(rally) && !terrainTileIsNull(goal.rotateRight()) && !wasThereBefore(goal.rotateRight())) {
            return goal.rotateRight();
        } else {
            return Direction.NONE;
        }
    }

    static boolean moveIsGood(Direction dir) {
        //iff: square is open, square is traversable, square is not too close to tower (unless we are attacking)
        return rc.canMove(dir) && (attack || !isInTowerRange(rc.getLocation().add(dir)));
    }

    static boolean terrainTileIsNull(Direction dir) {
        return !rc.senseTerrainTile(rc.getLocation().add(dir)).isTraversable();
    }

    /**
     * ****** Other ********
     */
    static void shareSupplies() throws GameActionException {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(), GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, myTeam);
        double lowestSupply = rc.getSupplyLevel();
        double transferAmount = 0;
        MapLocation suppliesToThisLocation = null;
        for (RobotInfo ri : nearbyAllies) {
            if (ri.supplyLevel < lowestSupply) {
                lowestSupply = ri.supplyLevel;
                if (getThreatLevel(ri.type) != 10 && getThreatLevel(ri.type) != 0) {
                    if (getThreatLevel(rc.getType()) != 10 && getThreatLevel(rc.getType()) != 0) {
                        transferAmount = (rc.getSupplyLevel() - ri.supplyLevel) / 2;
                    } else {
                        if (rc.getSupplyLevel() > 7500) {
                            transferAmount = 7500;
                        } else {
                            transferAmount = rc.getSupplyLevel();
                        }
                    }
                }
                suppliesToThisLocation = ri.location;
            }
        }
        if (suppliesToThisLocation != null) {
            rc.transferSupplies((int) transferAmount, suppliesToThisLocation);
        }
    }

    static void dumpSupply() throws GameActionException {
        RobotInfo nearAllies[] = rc.senseNearbyRobots(15, myTeam);
        if (nearAllies.length > 0) {
            switch (rc.getType()) {
                case BEAVER:
                    if (rc.getSupplyLevel() > 50)
                        rc.transferSupplies((int) rc.getSupplyLevel() - 50, nearAllies[0].location);
                    break;
                case MINER:
                    if (rc.getSupplyLevel() > 40)
                        rc.transferSupplies((int) rc.getSupplyLevel() - 40, nearAllies[0].location);
                    break;
                case COMPUTER:
                    if (rc.getSupplyLevel() > 10)
                        rc.transferSupplies((int) rc.getSupplyLevel() - 10, nearAllies[0].location);
                    break;
                case BASHER:
                    if (rc.getSupplyLevel() > 30)
                        rc.transferSupplies((int) rc.getSupplyLevel() - 30, nearAllies[0].location);
                    break;
                case COMMANDER:
                    if (rc.getSupplyLevel() > 25)
                        rc.transferSupplies((int) rc.getSupplyLevel() - 25, nearAllies[0].location);
                    break;
                case LAUNCHER:
                    if (rc.getSupplyLevel() > 125)
                        rc.transferSupplies((int) rc.getSupplyLevel() - 125, nearAllies[0].location);
                    break;
                case MISSILE:
                    rc.transferSupplies((int) rc.getSupplyLevel(), nearAllies[0].location);
                    break;
                case SOLDIER:
                    if (rc.getSupplyLevel() > 25)
                        rc.transferSupplies((int) rc.getSupplyLevel() - 25, nearAllies[0].location);
                    break;
                case TANK:
                    if (rc.getSupplyLevel() > 75)
                        rc.transferSupplies((int) rc.getSupplyLevel() - 75, nearAllies[0].location);
                    break;
                case DRONE:
                    if (rc.getSupplyLevel() > 50)
                        rc.transferSupplies((int) rc.getSupplyLevel() - 50, nearAllies[0].location);
                    break;
                default: //building probably
                    rc.transferSupplies((int) rc.getSupplyLevel(), nearAllies[0].location);
                    break;
            }

        }
    }

    //for building type rc's: splits all this rc's supply between all moving units & buildings that are further from the HQ than this one
    //also uses too many bytecodes, so shouldn't be used right now
    static void transferSupplies() throws GameActionException {
        if (rc.getSupplyLevel() > 10) {
            RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(), GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, myTeam);
            MapLocation[] locations = new MapLocation[nearbyAllies.length];
            int locIndex = 0;
            int supplyAmount;

            for (int i = 0; i < nearbyAllies.length; i++) {
                if (shouldTransfer(nearbyAllies[i].location, nearbyAllies[i].type)) {
                    locations[locIndex] = nearbyAllies[i].location;
                    locIndex++;
                }
            }
            if (locIndex != 0) {
                supplyAmount = (int) rc.getSupplyLevel() / locIndex;
                for (int i = 0; i < locIndex; i++) {
                    try {
                        rc.transferSupplies(supplyAmount, locations[i]);
                    } catch (Exception e) {
                        System.out.println("Failed to transfer " + supplyAmount + " supply from" + rc.getLocation() + "to " + locations[i]);
                    }
                }
            }
        }
    }

    static boolean shouldTransfer(MapLocation location, RobotType type) throws GameActionException {
        if (type == RobotType.HQ) {
            return false;
        }
        if (type == RobotType.BARRACKS || type == RobotType.TOWER || type == RobotType.TANKFACTORY ||
                type == RobotType.MINERFACTORY || type == RobotType.SUPPLYDEPOT || type == RobotType.HANDWASHSTATION) {
            if (location.distanceSquaredTo(myHQ) < rc.getLocation().distanceSquaredTo(myHQ)) {
                return false;
            }
        }
        return true;

    }

    static MapLocation findBestOre() throws GameActionException {
        MapLocation locs[] = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), 9);
        MapLocation currentLoc = rc.getLocation();
        MapLocation bestLoc = currentLoc;
        double bestOre = rc.senseOre(currentLoc);
        double curOre;
        for (int i = 0; i < locs.length; i++) {
            if (!rc.isLocationOccupied(locs[i])) {
                curOre = rc.senseOre(locs[i]);
                if ((curOre > bestOre) || (curOre == bestOre && bestLoc.distanceSquaredTo(currentLoc) > locs[i].distanceSquaredTo(currentLoc))) {
                    bestOre = curOre;
                    bestLoc = locs[i];
                }
            }
        }
        //if you are at the broadcasted location and the broadcasted ore amount is outdated, update ore count
        if (currentLoc.distanceSquaredTo(new MapLocation(rc.readBroadcast(201), rc.readBroadcast(202))) < 20 &&
                (int) bestOre < rc.readBroadcast(200)) {
            rc.broadcast(200, (int) bestOre);
        }

        //if your ore is the best, tell 'em about it
        if (bestOre > rc.readBroadcast(200) || (Clock.getRoundNum() > rc.readBroadcast(203) + 10 && bestOre == rc.readBroadcast(200))) {
            rc.broadcast(200, (int) bestOre);
            rc.broadcast(201, bestLoc.x);
            rc.broadcast(202, bestLoc.y);
            rc.broadcast(203, Clock.getRoundNum());
        }
        ;

        return bestLoc;
    }

    static boolean isInTowerRange(MapLocation move) {
        MapLocation[] towers = rc.senseEnemyTowerLocations();
        for (int i = 0; i < towers.length; i++) {
            if (towers[i].distanceSquaredTo(move) < 25) {
                return true;
            }
        }
        //assume HQ won't be a problem
        return false;
    }

    static boolean mapLocationIsOnGrid(MapLocation loc) {
        return Math.abs(loc.x) % 2 == Math.abs(loc.y) % 2;
    }

    static boolean isTied() {
        return rc.senseTowerLocations().length == rc.senseEnemyTowerLocations().length;
    }

    static int getTowerChannel(MapLocation loc) throws GameActionException {
        for (int i = 301; i <= 316; i += 3) {
            if (rc.readBroadcast(i) == loc.x && rc.readBroadcast(i + 1) == loc.y) {
                return i + 2;
            }
        }
        return -1;
    }

    static void broadcastLocation(MapLocation loc) throws GameActionException {
        rc.broadcast(50, loc.x);
        rc.broadcast(51, loc.y);
    }

    static MapLocation checkStatuses() throws GameActionException {
        for (int i = 300; i <= 318; i += 3) {
            if (rc.readBroadcast(i) >= minimunThreat) {
                if (i == 300) {
                    return myHQ;
                } else {
                    return new MapLocation(rc.readBroadcast(i - 2), rc.readBroadcast(i - 1));
                }
            }
        }
        return enemyHQ;
    }

    static MapLocation getBroadcastedRally() throws GameActionException {
        int[] broadcasted = {rc.readBroadcast(50), rc.readBroadcast(51)};
        if (broadcasted[0] == 0 && broadcasted[1] == 0) {
            return rally;
        } else {
            return new MapLocation(broadcasted[0], broadcasted[1]);
        }

    }
}

