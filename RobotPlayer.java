package tehnummurone;

import battlecode.common.*;
import java.util.*;

public class RobotPlayer {
	static RobotController rc;
	static Team myTeam;
	static Team enemyTeam;
	static int myRange;
	static Random rand;
	static RobotInfo[] myRobots;
	static Direction[] directions = {Direction.NORTH, Direction.NORTH_EAST, Direction.EAST, 
										Direction.SOUTH_EAST, Direction.SOUTH, Direction.SOUTH_WEST, 
										Direction.WEST, Direction.NORTH_WEST};
	
	public static void run(RobotController tomatojuice) {
		rc = tomatojuice;
        rand = new Random(rc.getID());

		myRange = rc.getType().attackRadiusSquared;
		MapLocation enemyLoc = rc.senseEnemyHQLocation();
        Direction lastDirection = null;
		myTeam = rc.getTeam();
		enemyTeam = myTeam.opponent();

		while(true) {
            try {
                rc.setIndicatorString(0, "I am a " + rc.getType());
				rc.setIndicatorString(1, "");
            } catch (Exception e) {
                System.out.println("Unexpected exception");
                e.printStackTrace();
            }
			
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
			}
		}
		rc.broadcast(0, numBeavers);
		rc.broadcast(1, numSoldiers);
		rc.broadcast(2, numBashers);
		rc.broadcast(100, numBarracks);

		if (rc.isWeaponReady()) {
			attackSomething();
		}

		if (rc.isCoreReady() && rc.getTeamOre() >= 100 && fate < Math.pow(1.2,12-numBeavers)*10000) {
			trySpawn(rc.getLocation().directionTo(rc.senseEnemyHQLocation()), RobotType.BEAVER);
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
			tryMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
		}
	}
	
	static void soldier() throws GameActionException {
		if (rc.isWeaponReady()) {
			attackSomething();
		}
		if (rc.isCoreReady()) {
			tryMove(rc.getLocation().directionTo(rc.senseEnemyHQLocation()));
		}
	}
	
	static void beaver() throws GameActionException {
		if (rc.isWeaponReady()) {
			attackSomething();
		}
		if (rc.isCoreReady()) {
			if (rc.getTeamOre() >= 400) {
				tryBuild(directions[rand.nextInt(8)], RobotType.BARRACKS);
			} else if (rc.senseOre(rc.getLocation()) > 0) {
				rc.mine();
			} else {
				tryMove(findCowardlyMove());
			}
		}
	}
	
	static void barracks() throws GameActionException {
		int fate = rand.nextInt(10000);

		// get information broadcasted by the HQ
		int numBeavers = rc.readBroadcast(0);
		int numSoldiers = rc.readBroadcast(1);
		int numBashers = rc.readBroadcast(2);

		if (rc.isCoreReady() && rc.getTeamOre() >= 60 && fate < Math.pow(1.2,15-numSoldiers-numBashers+numBeavers)*10000) {
			if (rc.getTeamOre() > 80 && fate % 2 == 0) {
				trySpawn(rc.getLocation().directionTo(rc.senseEnemyHQLocation()),RobotType.BASHER);
			} else {
				trySpawn(rc.getLocation().directionTo(rc.senseEnemyHQLocation()),RobotType.SOLDIER);
			}
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
	static void tryMove(Direction d) throws GameActionException {
		int offsetIndex = 0;
		int[] offsets = {0, 1, -1, 2, -2};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
			rc.move(directions[(dirint + offsets[offsetIndex] + 8) % 8]);
		} else {
			rc.setIndicatorString(1, "I am stuck");
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
	
	static Direction findCowardlyMove(){
		switch(rc.getLocation().directionTo(rc.senseEnemyHQLocation())) {
			case NORTH:
				return Direction.SOUTH;
			case NORTH_EAST:
				return Direction.SOUTH_WEST;
			case EAST:
				return Direction.WEST;
			case SOUTH_EAST:
				return Direction.NORTH_WEST;
			case SOUTH:
				return Direction.NORTH;
			case SOUTH_WEST:
				return Direction.NORTH_EAST;
			case WEST:
				return Direction.EAST;
			case NORTH_WEST:
				return Direction.SOUTH_EAST;
			default:
				return Direction.NORTH;
		}
		
	}
}
