package tehnummurone;

import battlecode.common.*;
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
		rc.setIndicatorString(0, "I am a " + rc.getType());

		while(true) {
			rc.setIndicatorString(1, "");
			
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
			}else if (rc.getType() == RobotType.SUPPLYDEPOT){
				//do nothing
                        }else if (rc.getType() == RobotType.TANKFACTORY){
				try {
					tankFactory();
				} catch (Exception e) {
					System.out.println("Tank Factory Exception");
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
		int numSupplyDepots = 0;
                int numTankFactories = 0;
                int numTanks = 0;
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
                        } else if (type == RobotType.TANKFACTORY){
				numTankFactories++;
			}
		}
		rc.broadcast(0, numBeavers);
		rc.broadcast(1, numSoldiers);
		rc.broadcast(2, numBashers);
                rc.broadcast(3, numTanks); 
		rc.broadcast(99, numSupplyDepots);
		rc.broadcast(100, numBarracks);
                rc.broadcast(101, numTankFactories);
		
		MapLocation closestTower = findClosestEnemyTower();
		rc.broadcast(50, closestTower.x);
		rc.broadcast(51, closestTower.y);

		if (rc.isWeaponReady()) {
			attackSomething();
		}

		if (rc.isCoreReady() && rc.getTeamOre() >= 100 && numBeavers<20 && fate < Math.pow(1.2,12-numBeavers)*10000) {
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
        static void tank() throws GameActionException {
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
			if (rc.getTeamOre() >= 400 && rc.readBroadcast(100)<3) {
				tryBuild(directions[rand.nextInt(8)], RobotType.BARRACKS);
			} else if (rc.getTeamOre() >= 100 && rc.readBroadcast(99)<1){ 
				tryBuild(rc.getLocation().directionTo(myHQ), RobotType.SUPPLYDEPOT);
                        } else if (rc.getTeamOre() >= 500 && rc.readBroadcast(100)>1 && rc.readBroadcast(101)<1 ){ 
				tryBuild(rc.getLocation().directionTo(myHQ), RobotType.TANKFACTORY);
                        }
			 else if (distanceBetween(rc.getLocation(), myHQ) < 4) { 
                             
				if(myDirection == null) {
					int fate = rand.nextInt(80);
					if (fate < 10) {
						myDirection=rc.getLocation().directionTo(enemyHQ).rotateLeft();
					} else if (fate < 20) {
						myDirection=rc.getLocation().directionTo(enemyHQ).rotateRight();
					} else if (fate < 30) {
						myDirection=rc.getLocation().directionTo(enemyHQ).rotateRight().rotateRight();
                                        } else if (fate < 40) {
						myDirection=rc.getLocation().directionTo(enemyHQ).rotateLeft().rotateLeft();
                                        } else if (fate < 50) {
						myDirection=rc.getLocation().directionTo(enemyHQ).rotateLeft().rotateLeft().rotateLeft();
                                        } else if (fate < 60) {
						myDirection=rc.getLocation().directionTo(enemyHQ).rotateRight().rotateRight().rotateRight();
                                        } else if (fate < 70){
						myDirection=rc.getLocation().directionTo(enemyHQ).opposite();
					} else {
						myDirection=rc.getLocation().directionTo(enemyHQ);
					}
				}
				tryMove(myDirection);
                         }else if (rc.senseOre(rc.getLocation()) > 5) {
				rc.mine();
                                
                        }else if (distanceBetween(rc.getLocation(), myHQ) < 20){
                                int fate = rand.nextInt(640);
                                if (fate < 10) {
						myDirection=myDirection.rotateLeft();
                                                tryMove(myDirection);
					} else if (fate < 20) {
						myDirection=myDirection.rotateRight();
                                                tryMove(myDirection);
					} else if (fate < 30) {
						myDirection=myDirection.rotateRight().rotateRight();
                                                tryMove(myDirection);
                                        } else if (fate < 40) {
						myDirection=myDirection.rotateLeft().rotateLeft();
                                                tryMove(myDirection);
                                        } else if (fate < 50) {
						myDirection=myDirection.rotateLeft().rotateLeft().rotateLeft();
                                                tryMove(myDirection);
                                        } else if (fate < 60) {
						myDirection=myDirection.rotateRight().rotateRight().rotateRight();
                                                tryMove(myDirection);
                                        } else if (fate < 70){
						myDirection=myDirection.opposite();
                                                tryMove(myDirection);
					} else if (fate < 80) {
						
                                                tryMove(myDirection);
					}

                        }else if (distanceBetween(rc.getLocation(), myHQ) >= 20){
                                int fate = rand.nextInt(2);
                                if (fate == 1){
                                    myDirection = rc.getLocation().directionTo(myHQ);
                                    tryMove(myDirection);
                                }
//                        }else if (rc.senseOre(rc.getLocation()) > 0) {
//				rc.mine();
			}else if (rc.senseOre(rc.getLocation()) <= 0) {
                            
				int fate = rand.nextInt(8);
                                int fateInit = fate;
                                boolean looped = false;
                                while ((rc.senseOre(rc.getLocation().add(directions[fate])) == 0 || !rc.canMove(directions[fate])) && !looped){
                                    fate = (fate + 1) %8;
                                    if (fate == fateInit){
                                        looped = true;
                                    }
                                   
                                }
                                if (!looped){
                                    tryMove(directions[fate]);
                                }else{
                                    tryMove(directions[fate]);    
                                }
                                
                        } else { //run awwayy!
				tryMove(rc.getLocation().directionTo(myHQ).opposite());
			}
			//TODO: search for ore
		}
	}
	
	static void barracks() throws GameActionException {
		int fate = rand.nextInt(10000);
                    //hello?
		// get information broadcasted by the HQ
		int numBeavers = rc.readBroadcast(0);
		int numSoldiers = rc.readBroadcast(1);
		int numBashers = rc.readBroadcast(2);
                int numTanks = rc.readBroadcast(3);
//&& rc.readBroadcast(101)>1 
		if (rc.isCoreReady()  && rc.getTeamOre() >= 80 && fate < Math.pow(1.2,15-numSoldiers-numBashers+numBeavers)*10000 && (numSoldiers + numBashers) < 20 ) {
			if (rc.getTeamOre() > 80 && fate % 2 == 0) {
				trySpawn(rc.getLocation().directionTo(enemyHQ),RobotType.BASHER);//uncommented this code
			} else {
				trySpawn(rc.getLocation().directionTo(enemyHQ),RobotType.SOLDIER);
			}
		}
	}
        static void tankFactory() throws GameActionException {
            int numTanks = rc.readBroadcast(3);
            if (rc.isCoreReady() && rc.getTeamOre() >= 250){
                trySpawn(rc.getLocation().directionTo(enemyHQ),RobotType.TANK);
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
		int[] offsets = {0,1,-1,2,-2};
		int dirint = directionToInt(d);
		boolean blocked = false;
		while (offsetIndex < 5 && !rc.canMove(directions[(dirint + offsets[offsetIndex] + 8) % 8])) {
			offsetIndex++;
		}
		if (offsetIndex < 5) {
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
			tryMove(rc.getLocation().directionTo(rally));
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
}
