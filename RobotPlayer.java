package team166;

import battlecode.client.viewer.AbstractDrawObject.RobotInfo;
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
    static Direction myDirection;
    static boolean clockwise;
    static boolean bug;
    static int howClose;
    static int[][] surrounding;
    static Random rand;
    static RobotInfo[] myRobots;
    static boolean attack;
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

        rc.setIndicatorString(0, "I am a " + rc.getType());

        while (true) {
            //rc.setIndicatorString(1, "");

            if (rc.getType() == RobotType.HQ) {
                try {
                    hq();
                } catch (Exception e) {
                    System.out.println("HQ Exception");
                    e.printStackTrace();
                }
            } else if (rc.getType() == RobotType.TOWER) {
                try {
                    tower();
                } catch (Exception e) {
                    System.out.println("Tower Exception");
                    e.printStackTrace();
                }
            } else if (rc.getType() == RobotType.BASHER) {
                try {
                    //basher code
                    basher();
                } catch (Exception e) {
                    System.out.println("Basher Exception");
                    e.printStackTrace();
                }
            } else if (rc.getType() == RobotType.SOLDIER) {
                try {
                    soldier();
                } catch (Exception e) {
                    System.out.println("Soldier Exception");
                    e.printStackTrace();
                }
            } else if (rc.getType() == RobotType.TANK) {
                try {
                    tank();
                } catch (Exception e) {
                    System.out.println("Tank Exception");
                    e.printStackTrace();
                }
            } else if (rc.getType() == RobotType.BEAVER) {
                try {
                    beaver();
                } catch (Exception e) {
                    System.out.println("Beaver Exception");
                    e.printStackTrace();
                }
            } else if (rc.getType() == RobotType.BARRACKS) {
                try {
                    barracks();
                } catch (Exception e) {
                    System.out.println("Barracks Exception");
                    e.printStackTrace();
                }
            } else if (rc.getType() == RobotType.SUPPLYDEPOT) {
                //do nothing
            } else if (rc.getType() == RobotType.TANKFACTORY) {
                try {
                    tankFactory();
                } catch (Exception e) {
                    System.out.println("Tank Factory Exception");
                    e.printStackTrace();
                }
            } else if (rc.getType() == RobotType.MINERFACTORY) {
                try {
                    minerFactory();
                } catch (Exception e) {
                    System.out.println("Miner Factory Exception");
                    e.printStackTrace();
                }
            } else if (rc.getType() == RobotType.MINER) {
                try {
                    miner();
                } catch (Exception e) {
                    System.out.println("Miner Exception");
                    e.printStackTrace();
                }
            } else {
                System.out.println("... I don't know... help");
            }

            rc.yield();
        }
    }

    /**
     * ******Robot Type Methods********
     */
    static void hq() throws GameActionException {
        transferSupplies();
        int fate = rand.nextInt(10000);
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
        for (RobotInfo r : myRobots) {
            RobotType type = r.type;
            if (type == RobotType.SOLDIER) {
                numSoldiers++;
            } else if (type == RobotType.BASHER) {
                numBashers++;
            } else if (type == RobotType.BEAVER) {
                numBeavers++;
            } else if (type == RobotType.MINER) {
                numMiners++;
            } else if (type == RobotType.BARRACKS) {
                numBarracks++;
            } else if (type == RobotType.SUPPLYDEPOT) {
                numSupplyDepots++;
            } else if (type == RobotType.TANKFACTORY) {
                numTankFactories++;
            } else if (type == RobotType.MINERFACTORY) {
                numMinerFactories++;
            }
        }
        rc.broadcast(0, numBeavers);
        rc.broadcast(1, numSoldiers);
        rc.broadcast(2, numBashers);
        rc.broadcast(3, numTanks);
        rc.broadcast(4, numMiners);
        rc.broadcast(99, numSupplyDepots);
        rc.broadcast(100, numBarracks);
        rc.broadcast(101, numTankFactories);
        rc.broadcast(102, numMinerFactories);

        MapLocation closestTower = findClosestEnemyTower();
        rc.broadcast(50, closestTower.x);
        rc.broadcast(51, closestTower.y);

        if (rc.isWeaponReady()) {
            attackSomething();
        }

        if (rc.isCoreReady() && rc.getTeamOre() >= 100 && numBeavers < 2) {
            trySpawn(rc.getLocation().directionTo(enemyHQ), RobotType.BEAVER);
        }
    }

    static void tower() throws GameActionException {
        transferSupplies();
        if (rc.isWeaponReady()) {
            attackSomething();
        }
        dumpSupply();
    }

    static void basher() throws GameActionException {
        transferSupplies();
        RobotInfo[] adjacentEnemies = rc.senseNearbyRobots(2, enemyTeam);

        if (rc.isCoreReady()) {
            moveToRally();
        }
        dumpSupply();
    }

    static void soldier() throws GameActionException {
        transferSupplies();
        if (rc.isWeaponReady()) {
            attackSomething();
        }
        if (rc.isCoreReady()) {
            moveToRally();
        }
        dumpSupply();
    }

    static void tank() throws GameActionException {
        transferSupplies();
        if (rc.isWeaponReady()) {
            attackSomething();
        }
        if (rc.isCoreReady()) {
            moveToRally();
        }
        dumpSupply();
    }

    static void miner() throws GameActionException {
        transferSupplies();
        if (rc.isWeaponReady()) {
            attackSomething();
        }
        if (rc.isCoreReady()) {
            if (rc.canMine() && rc.senseOre(rc.getLocation()) > 5) {
                rc.setIndicatorString(1, "Mining...");
                rc.mine();
            } else {

                MapLocation goal = findBestOre();
                if (goal != rc.getLocation()) {
                    rc.setIndicatorString(1, "Moving to sensed square");
                    bug(goal);
                } else if (rc.readBroadcast(201) != 0 && rc.readBroadcast(202) != 0) {
                    rc.setIndicatorString(1, "Moving to broadcasted square: " + rc.readBroadcast(201) + " " + rc.readBroadcast(202));
                    bug(new MapLocation(rc.readBroadcast(201), rc.readBroadcast(202)));
                } else {
                    rc.setIndicatorString(1, "Moving in random direction");
                }
            }
        }
        dumpSupply();
    }

    static void beaver() throws GameActionException {
        transferSupplies();
        if (rc.isWeaponReady()) {
            attackSomething();
        }
        if (rc.isCoreReady()) {
            if (rc.getTeamOre() >= 500 && rc.readBroadcast(102) < 1) {
                tryBuild(directions[rand.nextInt(8)], RobotType.MINERFACTORY);
            } else if (rc.getTeamOre() >= 400 && rc.readBroadcast(100) < 2 && rc.readBroadcast(102) > 0) {
                tryBuild(directions[rand.nextInt(8)], RobotType.BARRACKS);
            } else if (rc.getTeamOre() >= 100 && rc.readBroadcast(99) < 1 && rc.readBroadcast(102) > 0) {
                tryBuild(rc.getLocation().directionTo(myHQ), RobotType.SUPPLYDEPOT);
            } else if (rc.getTeamOre() >= 500 && rc.readBroadcast(100) > 1 && rc.readBroadcast(101) < 1 && rc.readBroadcast(102) > 0) {
                tryBuild(rc.getLocation().directionTo(myHQ), RobotType.TANKFACTORY);
            } else if (distanceBetween(rc.getLocation(), myHQ) < 4) {

                if (myDirection == null) {
                    int fate = rand.nextInt(80);
                    if (fate < 10) {
                        myDirection = rc.getLocation().directionTo(enemyHQ).rotateLeft();
                    } else if (fate < 20) {
                        myDirection = rc.getLocation().directionTo(enemyHQ).rotateRight();
                    } else if (fate < 30) {
                        myDirection = rc.getLocation().directionTo(enemyHQ).rotateRight().rotateRight();
                    } else if (fate < 40) {
                        myDirection = rc.getLocation().directionTo(enemyHQ).rotateLeft().rotateLeft();
                    } else if (fate < 50) {
                        myDirection = rc.getLocation().directionTo(enemyHQ).rotateLeft().rotateLeft().rotateLeft();
                    } else if (fate < 60) {
                        myDirection = rc.getLocation().directionTo(enemyHQ).rotateRight().rotateRight().rotateRight();
                    } else if (fate < 70) {
                        myDirection = rc.getLocation().directionTo(enemyHQ).opposite();
                    } else {
                        myDirection = rc.getLocation().directionTo(enemyHQ);
                    }
                }
                tryMove(myDirection, "");
            } else if (rc.senseOre(rc.getLocation()) > 5) {
                rc.mine();

            } else if (distanceBetween(rc.getLocation(), myHQ) < 20) {
                int fate = rand.nextInt(640);
                if (fate < 10) {
                    myDirection = myDirection.rotateLeft();
                    tryMove(myDirection, "");
                } else if (fate < 20) {
                    myDirection = myDirection.rotateRight();
                    tryMove(myDirection, "");
                } else if (fate < 30) {
                    myDirection = myDirection.rotateRight().rotateRight();
                    tryMove(myDirection, "");
                } else if (fate < 40) {
                    myDirection = myDirection.rotateLeft().rotateLeft();
                    tryMove(myDirection, "");
                } else if (fate < 50) {
                    myDirection = myDirection.rotateLeft().rotateLeft().rotateLeft();
                    tryMove(myDirection, "");
                } else if (fate < 60) {
                    myDirection = myDirection.rotateRight().rotateRight().rotateRight();
                    tryMove(myDirection, "");
                } else if (fate < 70) {
                    myDirection = myDirection.opposite();
                    tryMove(myDirection, "");
                } else if (fate < 80) {

                    tryMove(myDirection, "");
                }

            } else if (distanceBetween(rc.getLocation(), myHQ) >= 20) {
                int fate = rand.nextInt(2);
                if (fate == 1) {
                    myDirection = rc.getLocation().directionTo(myHQ);
                    tryMove(myDirection, "");
                }
//                        }else if (rc.senseOre(rc.getLocation()) > 0) {
//				rc.mine();
            } else if (rc.senseOre(rc.getLocation()) <= 0) {

                int fate = rand.nextInt(8);
                int fateInit = fate;
                boolean looped = false;
                while ((rc.senseOre(rc.getLocation().add(directions[fate])) == 0 || !rc.canMove(directions[fate])) && !looped) {
                    fate = (fate + 1) % 8;
                    if (fate == fateInit) {
                        looped = true;
                    }

                }
                if (!looped) {
                    tryMove(directions[fate], "");
                } else {
                    tryMove(directions[fate], "");
                }

            } else { //run awwayy!
                tryMove(rc.getLocation().directionTo(myHQ).opposite(), "");
            }
            //TODO: search for ore
        }
        dumpSupply();
    }

    static void barracks() throws GameActionException {
        transferSupplies();
        int fate = rand.nextInt(10000);
        // get information broadcasted by the HQ
        int numBeavers = rc.readBroadcast(0);
        int numSoldiers = rc.readBroadcast(1);
        int numBashers = rc.readBroadcast(2);
        int numTanks = rc.readBroadcast(3);
        int numTankFactories = rc.readBroadcast(101);
//&& rc.readBroadcast(101)>1 
        if (rc.isCoreReady() && rc.getTeamOre() >= 80) {
            if (rc.getTeamOre() > 80 && numTankFactories == 0) {//&& fate % 2 == 0
                trySpawn(rc.getLocation().directionTo(enemyHQ), RobotType.SOLDIER);
            } else if ((rc.isCoreReady() && rc.getTeamOre() > 80 && numTanks >= numSoldiers) || (rc.isCoreReady() && rc.getTeamOre() > 500)) {
                trySpawn(rc.getLocation().directionTo(enemyHQ), RobotType.SOLDIER);
            }
//                        else {
//				trySpawn(rc.getLocation().directionTo(enemyHQ),RobotType.SOLDIER);
//			}
        }
        dumpSupply();
    }

    static void tankFactory() throws GameActionException {
        transferSupplies();
        int numTanks = rc.readBroadcast(3);
        if ((rc.isCoreReady() && rc.getTeamOre() >= 250) || (rc.isCoreReady() && rc.getTeamOre() > 1000)) {
            trySpawn(rc.getLocation().directionTo(enemyHQ), RobotType.TANK);
        }
        dumpSupply();
    }

    static void minerFactory() throws GameActionException {
        if (rc.isCoreReady() && rc.getTeamOre() >= 50 && rc.readBroadcast(4) < 15) { // TODO make function of map size
            trySpawn(rc.getLocation().directionTo(enemyHQ), RobotType.MINER);
        }
        dumpSupply();

    }
    static void dumpSupply(){
        if (rc.getHealth() <= 5){
            RobotInfo nearAllies[] = senseNearbyRobots(15, myTeam);
            if(nearAllies[0] != null){
                switch(nearAllies[0].getType()){
            case BEAVER:
                rc.transferSupplies(rc.supplyLevel - 50, nearAllies[0].getLocation());
                break;
            case MINER:
                rc.transferSupplies(rc.supplyLevel - 40, nearAllies[0].getLocation());
                break;
            case COMPUTER:
                rc.transferSupplies(rc.supplyLevel - 10, nearAllies[0].getLocation());
                break;
            case BASHER:
                rc.transferSupplies(rc.supplyLevel - 30, nearAllies[0].getLocation());
                break;
            case COMMANDER:
                rc.transferSupplies(rc.supplyLevel - 25, nearAllies[0].getLocation());
                break;
            case LAUNCHER:
                rc.transferSupplies(rc.supplyLevel - 125, nearAllies[0].getLocation());
                break;
            case MISSILE:
                rc.transferSupplies(rc.supplyLevel, nearAllies[0].getLocation());
                break;
            case SOLDIER:
                rc.transferSupplies(rc.supplyLevel - 25, nearAllies[0].getLocation());
                break;
            case TANK:
                rc.transferSupplies(rc.supplyLevel - 75, nearAllies[0].getLocation());
                break;
            case DRONE:
                rc.transferSupplies(rc.supplyLevel - 50, nearAllies[0].getLocation());
                break;
            default: //building probably
                rc.transferSupplies(rc.supplyLevel, nearAllies[0].getLocation());
                break;
        }
                
            }
        }
        
    }

    /**
     * ******Attack, Movement, and Creation Methods********
     */
    // This method will attack an enemy in sight, if there is one
    static void attackSomething() throws GameActionException {
        RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
        if(enemies.length>0){
            RobotInfo target = enemies[0];
            int targetThreat = getThreatLevel(target.type);
            for(int i = 1; i < enemies.length; i++){
                if(getThreatLevel(enemies[i].type) > targetThreat){
                    target = enemies[i];
                    targetThreat = getThreatLevel(enemies[i].type);
                } else if (getThreatLevel(enemies[i].type) == targetThreat){
                    if((enemies[i].location.distanceSquaredTo(rc.getLocation()) < 
                            target.location.distanceSquaredTo(rc.getLocation())) ||
                            (enemies[i].health < target.health)){
                        target = enemies[i];
                    }
                }
            }
            rc.attackLocation(target.location);
        }
    }
    static float getDamage(RobotType r){
         switch(r){
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
                return 0;
            case MISSILE:
                return 20; //temp
            case TOWER:
                return 10;
            case SOLDIER:
                return 4;
            case TANK:
                return 6.7;
            case DRONE:
                return 2.7;
            default: //building probably
                return 0;
        }
    }
    
    static int getThreatLevel(RobotType r){
        switch(r){
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
            if (rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8]))
                rc.setIndicatorString(1, info + " move good:" + directions[(dirint + offsets[offsetIndex] + 8) % 8]);
            else rc.setIndicatorString(1, info + " move bad:" + directions[(dirint + offsets[offsetIndex] + 8) % 8]);
            rc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
            //mapMove(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
            return true;
        } else {
            rc.setIndicatorString(1, info + "move failed");
            return false;
        }
    }

    static void moveToRally() throws GameActionException {
        int distanceToGoal = distanceBetween(rc.getLocation(), rally);
//        if(distanceToGoal < 10 && rc.readBroadcast(60)==0 ){
//            if(clockwise) rc.broadcast(60, 1);
//            else rc.broadcast(60, 2);
//        }

        if (attack || (distanceToGoal < 50 && Clock.getRoundNum() % 250 < 10)) {
            rally = new MapLocation(rc.readBroadcast(50), rc.readBroadcast(51));
            attack = true;
        }

        if (distanceToGoal > 2)
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
    static void tryBuild(Direction d, RobotType type) throws GameActionException {
        int offsetIndex = 0;
        int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
        int dirint = directionToInt(d);
        boolean blocked = false;
        while (offsetIndex < 8 && (!rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8]) ||
                (rc.getLocation().add(directions[(dirint + offsets[offsetIndex] + 8) % 8]).x % 2 != rc.getLocation().add(directions[(dirint + offsets[offsetIndex] + 8) % 8]).y % 2))) {
            offsetIndex++;
        }
        if (offsetIndex < 8) {
            rc.build(directions[(dirint + offsets[offsetIndex] + 8) % 8], type);
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

    static int distanceBetween(MapLocation a, MapLocation b) {
        return (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y);
    }

    static MapLocation getRally() {
        return new MapLocation((enemyHQ.x + myHQ.x) / 2,
                (enemyHQ.y + myHQ.y) / 2);

    }

    static MapLocation findClosestEnemyTower() {
        MapLocation[] towers = rc.senseEnemyTowerLocations();
        MapLocation closest = enemyHQ;
        int min_distance = distanceBetween(rally, enemyHQ);
        int cur;

        for (int i = 0; i < towers.length; i++) {
            cur = distanceBetween(rally, towers[i]);
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

    static boolean terrainTileIsNull(Direction dir) {
        return !rc.senseTerrainTile(rc.getLocation().add(dir)).isTraversable();
    }

    /**
     * ****** Other ********
     */
    static void transferSupplies() throws GameActionException {
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(rc.getLocation(), GameConstants.SUPPLY_TRANSFER_RADIUS_SQUARED, rc.getTeam());
        double lowestSupply = rc.getSupplyLevel();
        double transferAmount = 0;
        MapLocation suppliesToThisLocation = null;
        for (RobotInfo ri : nearbyAllies) {
            if (ri.supplyLevel < lowestSupply) {
                lowestSupply = ri.supplyLevel;
                transferAmount = (rc.getSupplyLevel() - ri.supplyLevel) / 2;
                suppliesToThisLocation = ri.location;
            }
        }
        if (suppliesToThisLocation != null) {
            rc.transferSupplies((int) transferAmount, suppliesToThisLocation);
        }
    }

    static MapLocation findBestOre() throws GameActionException {
        MapLocation locs[] = MapLocation.getAllMapLocationsWithinRadiusSq(rc.getLocation(), 20);
        MapLocation currentLoc = rc.getLocation();
        MapLocation bestLoc = currentLoc;
        double bestOre = 5;
        double curOre;
        for (int i = 0; i < locs.length; i++) {
            if (!rc.isLocationOccupied(locs[i])) {
                curOre = rc.senseOre(locs[i]);
                if ((curOre > bestOre) || (curOre == bestOre && distanceBetween(bestLoc, currentLoc) > distanceBetween(locs[i], currentLoc))) {
                    bestOre = curOre;
                    bestLoc = locs[i];
                }
            }
        }
        //if you are at the broadcasted location and the broadcasted ore amount is outdated, update ore count
        if (distanceBetween(currentLoc, new MapLocation(rc.readBroadcast(201), rc.readBroadcast(202))) < 25 &&
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

    static Direction dirCloserToRally(int closest, Direction goal) {
        if (closest > distanceBetween(rc.getLocation().add(goal), rally) && !terrainTileIsNull(goal) && !wasThereBefore(goal)) {
            return goal;
        } else if (closest > distanceBetween(rc.getLocation().add(goal.rotateLeft()), rally) && !terrainTileIsNull(goal.rotateLeft()) && !wasThereBefore(goal.rotateLeft())) {
            return goal.rotateLeft();
        } else if (closest > distanceBetween(rc.getLocation().add(goal.rotateRight()), rally) && !terrainTileIsNull(goal.rotateRight()) && !wasThereBefore(goal.rotateRight())) {
            return goal.rotateRight();
        } else {
            return Direction.NONE;
        }
    }

    static boolean moveIsGood(Direction dir) {
        //iff: square is open, square is traversable, square is not too close to tower
        return rc.canMove(dir) && (attack || !isInTowerRange(rc.getLocation().add(dir)));
        //return rc.canMove(dir) && !isInTowerRange(rc.getLocation().add(dir));
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
}
