package tehnummurone;

import battlecode.common.*;

import java.lang.Exception;
import java.util.*;
import java.lang.Math;

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
	static Random rand;
	static RobotInfo[] myRobots;
	static boolean attack;
	static boolean followEdge;
	static int[][] surrounding;
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, 
										Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, 
										Direction.WEST, Direction.NORTH_WEST};
	
	public static void run(RobotController tomatojuice) {
		rc = tomatojuice;
        rand = new Random(rc.getID());
		
		followEdge = false;
		attack = false;
		myRange = rc.getType().attackRadiusSquared;
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
        Direction lastDirection = null;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();
		myHQ = rc.senseHQLocation();
		enemyHQ = rc.senseEnemyHQLocation();
		rally = getRally();
		rc.setIndicatorString(0, "I am a " + rc.getType());
		surrounding = new int[3][3];

		while(true) {
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
			}else if (rc.getType() == RobotType.SUPPLYDEPOT){
				//do nothing
			} else {
				System.out.println("... I don't know... help");
			}
			
			rc.yield();
		}
	}
	/*Robot Type Methods*/
	static void hq() throws GameActionException {
		int fate = rand.nextInt(10000);
		myRobots = rc.senseNearbyRobots(999999, myTeam);
		int numSoldiers = 0;
		int numBashers = 0;
		int numBeavers = 0;
		int numBarracks = 0;
		int numSupplyDepots = 0;
		for (RobotInfo r : myRobots) {
			RobotType type = r.type;
			if (type == RobotType.SOLDIER) {
				numSoldiers++;
			} else if (type == RobotType.BASHER) {
				numBashers++;
			} else if (type == RobotType.BEAVER) {
				numBeavers++;
			} else if (type == RobotType.BARRACKS) {
				numBarracks++;
			} else if (type == RobotType.SUPPLYDEPOT){
				numSupplyDepots++;
			}
		}
		rc.broadcast(0, numBeavers);
		rc.broadcast(1, numSoldiers);
		rc.broadcast(2, numBashers);
		rc.broadcast(99, numSupplyDepots);
		rc.broadcast(100, numBarracks);
		
		MapLocation closestTower = findClosestEnemyTower();
		rc.broadcast(50, closestTower.x);
		rc.broadcast(51, closestTower.y);

		if (rc.isWeaponReady()) {
			attackSomething();
		}

		if (rc.isCoreReady() && rc.getTeamOre() >= 100 && numBeavers<30 && fate < Math.pow(1.2,12-numBeavers)*10000) {
			trySpawn(rc.getLocation().directionTo(enemyHQ), RobotType.BEAVER);
		}
	}
	
	static void tower() throws GameActionException {
		if (rc.isWeaponReady()) {
			attackSomething();
		}
	}
	
	static void basher() throws GameActionException {
		RobotInfo[] adjacentEnemies = rc.senseNearbyRobots(2, enemyTeam);
		
		// BASHERs attack automatically, so let's just move around mostly randomly
		if (rc.isCoreReady()) {
			moveToRally();
		}
	}
	
	static void soldier() throws GameActionException {
		if (rc.isWeaponReady()) {
			attackSomething();
		}
		if (rc.isCoreReady()){
			moveToRally();
		}

	}
	
	static void beaver() throws GameActionException {
		if (rc.isWeaponReady()) {
			attackSomething();
		}
		if (rc.isCoreReady()) {
			if (rc.getTeamOre() >= 400 && rc.readBroadcast(100)<2) {
				tryBuild(directions[rand.nextInt(8)], RobotType.BARRACKS);
			} else if (rc.getTeamOre() >= 100 && rc.readBroadcast(99)<3){
				tryBuild(rc.getLocation().directionTo(myHQ), RobotType.SUPPLYDEPOT);
			} else if (distanceBetween(rc.getLocation(), myHQ) < 4) {
				if(myDirection == null) {
					int fate = rand.nextInt(30);
					if (fate < 10) {
						myDirection=rc.getLocation().directionTo(enemyHQ).rotateLeft().rotateLeft();
					} else if (fate < 20) {
						myDirection=rc.getLocation().directionTo(enemyHQ).rotateRight().rotateRight();
					} else {
						myDirection=rc.getLocation().directionTo(enemyHQ).opposite();
					}
				}
				tryMove(myDirection);
			} else if (rc.senseOre(rc.getLocation()) > 0) {
				rc.mine();
			} else { //run awwayy!
				tryMove(rc.getLocation().directionTo(myHQ).opposite());
			}
			//TODO: search for ore
		}
	}
	
	static void barracks() throws GameActionException {
		int fate = rand.nextInt(10000);

		// get information broadcasted by the HQ
		int numBeavers = rc.readBroadcast(0);
		int numSoldiers = rc.readBroadcast(1);
		int numBashers = rc.readBroadcast(2);

		if (rc.isCoreReady() && rc.getTeamOre() >= 80 && fate < Math.pow(1.2,15-numSoldiers-numBashers+numBeavers)*10000) {
			//if (rc.getTeamOre() > 80 && fate % 2 == 0) {
				trySpawn(rc.getLocation().directionTo(enemyHQ),RobotType.BASHER);
//			} else {
//				trySpawn(rc.getLocation().directionTo(enemyHQ),RobotType.SOLDIER);
//			}
		}
	}
	
	/*Helper Methods*/
    // This method will attack an enemy in sight, if there is one
	static void attackSomething() throws GameActionException {
		RobotInfo[] enemies = rc.senseNearbyRobots(myRange, enemyTeam);
		if (enemies.length > 0) {
			rc.attackLocation(enemies[0].location);
		}
	}
    // This method will attempt to move in Direction d (or as close to it as possible)
	static boolean tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8 && !checkCanMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			mapMove(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
			rc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
			return true;
		} else {
			rc.setIndicatorString(1, "I am stuck");
			return false;
		}
	}
	
    // This method will attempt to spawn in the given direction (or as close to it as possible)
	static void trySpawn(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8 && !rc.canSpawn(directions[(dirint+offsets[offsetIndex]+8)%8], type)) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.spawn(directions[(dirint+offsets[offsetIndex] + 8) % 8], type);
		} else {
			rc.setIndicatorString(1, "I can't spawn things");
		}
	}
    // This method will attempt to build in the given direction (or as close to it as possible)
	static void tryBuild(Direction d, RobotType type) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0,1,-1,2,-2,3,-3,4};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 8 && !rc.canMove(directions[(dirint+offsets[offsetIndex]+8)%8])) {
			offsetIndex++;
		}
		if (offsetIndex < 8) {
			rc.build(directions[(dirint+offsets[offsetIndex]+8)%8], type);
		}
	}
	static int directionToInt(Direction d) {
		switch(d) {
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

	static int distanceBetween(MapLocation a, MapLocation b){
		return (int) Math.sqrt((a.x-b.x)*(a.x-b.x) + (a.y-b.y)*(a.y-b.y));
	}
	
	static void moveToRally() throws GameActionException{
		int distanceToGoal = distanceBetween(rc.getLocation(), rally);
		if(attack || (distanceToGoal < 10 && Clock.getRoundNum()%250<10)){
			rally = new MapLocation(rc.readBroadcast(50), rc.readBroadcast(51));
			attack = true;
			//TODO: should eventually go for nearest tower
		}
			
		if(distanceToGoal > 2)
			bugMove(rally);
		else
			rc.setIndicatorString(1, "waiting...");
		//TODO: while waiting, find nearish enemies and KILL THEM
	}
	
	static MapLocation getRally(){
		return new MapLocation((enemyHQ.x + myHQ.x)/2,
				(enemyHQ.y + myHQ.y)/2);
		
	}
	
	static MapLocation findClosestEnemyTower(){
		MapLocation[] towers = rc.senseEnemyTowerLocations();
		MapLocation closest = enemyHQ;
		int min_distance = distanceBetween(rally, enemyHQ);
		int cur;
		
		for(int i=0; i<towers.length; i++){
			cur = distanceBetween(rally, towers[i]);
			if(cur<min_distance){
				closest = towers[i];
				min_distance = cur;
			}
		}
		return closest;
		
		
	}
	
	static void mapMove(Direction move){
		surrounding = new int[3][3];
		switch(move.opposite()) {
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
	
	static boolean checkCanMove(Direction d){
		return rc.canMove(d) && !wasThereBefore(d);
	}
	
	static boolean wasThereBefore(Direction move){
		int result = -1;
		switch(move) {
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
	static MapLocation followSimpleEdge(MapLocation start, MapLocation wall, Direction prefered){
		int offsetIndex = 0;
		int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
		int dirint = directionToInt(prefered);
		while (offsetIndex < 8 &&
				(!wall.isAdjacentTo(start.add(directions[(dirint + offsets[offsetIndex] + 8) % 8])) ||
						!rc.senseTerrainTile(start.add(directions[(dirint + offsets[offsetIndex] + 8) % 8])).isTraversable() ||
						//!hasNeighboringNull(start.add(directions[(dirint+offsets[offsetIndex] + 8) % 8])) ||
						wasThereBefore(directions[(dirint + offsets[offsetIndex] + 8) % 8])
				)) {
			offsetIndex++;
		}
		if (offsetIndex < 8)
			return start.add(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
		else return start;
	}
	
	static MapLocation followHardEdge(MapLocation start, Direction prefered){
		MapLocation[] nulls = getAllNeighboringNull(start);
		int offsetIndex;
		
		int[] offsets = {0, 1, -1, 2, -2, 3, -3, 4};
		int dirint = directionToInt(prefered);
		for(int i=0; i<nulls.length; i++) {
			offsetIndex = 0;
			if (nulls[i] != null) {
				while (offsetIndex < 8 &&
						(!nulls[i].isAdjacentTo(start.add(directions[(dirint + offsets[offsetIndex] + 8) % 8])) ||
								!rc.senseTerrainTile(start.add(directions[(dirint + offsets[offsetIndex] + 8) % 8])).isTraversable() ||
								//!hasNeighboringNull(start.add(directions[(dirint+offsets[offsetIndex] + 8) % 8])) ||
								wasThereBefore(directions[(dirint + offsets[offsetIndex] + 8) % 8]))) {
					offsetIndex++;
				}
				if (offsetIndex < 8)
					return start.add(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
			}
		}
		return start;
	}
	
	static boolean hasNeighboringNull(MapLocation target){
		TerrainTile tile;
		for(int i=0; i< directions.length; i++){
			tile = rc.senseTerrainTile(target.add(directions[i]));
			if(!tile.isTraversable()){
				return true;
			}
		}
		return false;
	}
	
	static MapLocation getNeighboringNull(MapLocation target) {
		int index = 0;
		TerrainTile tile;
		for (int i=0; i<directions.length; i++){
			tile = rc.senseTerrainTile(target.add(directions[index]));
			if(!tile.isTraversable()){
				return target.add(directions[index]);
			}
			index = (index + 2);
			if(index == 8) index = 1;
		}
		System.out.println("I should not be here!");
		return null;
	}

	static MapLocation[] getAllNeighboringNull(MapLocation target) {
		int index = 0;
		int next = 0;
		MapLocation[] results = new MapLocation[8];
		TerrainTile tile;
		for (int i=0; i<directions.length; i++){
			tile = rc.senseTerrainTile(target.add(directions[index]));
			if(!tile.isTraversable()){
				results[next] = target.add(directions[index]);
				next++;
			}
			index = (index + 2);
			if(index == 8) index = 1;
		}
		return results;
	}

	static boolean bugMove(MapLocation m) throws GameActionException {
		MapLocation cur = rc.getLocation();
		Direction optimal = cur.directionTo(m);
		System.out.println("i'm trying to move");
		if(rc.canMove(optimal)){
			//I want to go to there
			rc.move(optimal);
		} else if (terrainTileIsNull(optimal)){
			//bug
			System.out.println("I found a wall");
			if(myDirection == null){
				System.out.println("finding direction");
				if(distanceBetween(rc.getLocation().add(optimal.rotateLeft()), m)<distanceBetween(rc.getLocation().add(optimal.rotateRight()), m)){
					myDirection = optimal.rotateRight();
					clockwise = false;
					rc.setIndicatorString(1, "turning counter-clockwise");
					
				}else{
					myDirection = optimal.rotateLeft();
					clockwise = true;
					rc.setIndicatorString(1, "turning clockwise");
				}
			}
			
			while(terrainTileIsNull(myDirection)){
				if(clockwise){
					myDirection = myDirection.rotateRight();
				} else {
					myDirection = myDirection.rotateLeft();
				}
			}
			
			if (clockwise){
				while(!terrainTileIsNull(myDirection.rotateRight())){
					myDirection = myDirection.rotateRight();
				}
			} else {
				while(!terrainTileIsNull(myDirection.rotateLeft())){
					myDirection = myDirection.rotateLeft();
				}
			}
			
			if(rc.canMove(myDirection)){
				rc.move(myDirection);
			}else{
				tryMove(myDirection);
			}
			
		}else {
			tryMove(optimal);
		}
		
		return true;
	}
	
	static boolean terrainTileIsNull(Direction dir){
		System.out.println("check that tile");
		return rc.senseTerrainTile(rc.getLocation().add(dir)).isTraversable();
	}
}
