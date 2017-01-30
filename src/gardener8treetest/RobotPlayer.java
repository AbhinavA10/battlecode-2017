package gardener8treetest;
import battlecode.common.*;
import java.util.Random;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Random rand = new Random();
    static boolean[] treesPlanted = new boolean[8];
    static boolean hasInitializedTP = false;
    static boolean hasMoved = false;
    static boolean movingBackToStart = false;
    static boolean foundStartLoc = false;
    static Direction[] dirList = new Direction[8]; // LEFT = 0, DOWN = 2, etc.
    static TreeInfo nearbyTrees[];
    static RobotInfo nearbyRobots[], nearbyEnemies[];
    static MapLocation startLoc;
    static MapLocation currentLoc;
    static float minTreeHealth = 50;
    static int currentDirection = 1;
    static int treeID;

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        currentLoc = rc.getLocation();
        if(!hasInitializedTP){
            hasInitializedTP = true;
            treesPlanted = initTP();
        }
        dirList = initDirList();
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
        }
    }



    static void runGardener() throws GameActionException {
        while (true) {
            try {
                int nCount = 0;
                if(!foundStartLoc) {
                    while (!rc.hasMoved() && nCount != 4) {
                        if (!tryMove(dirList[currentDirection], 5, 2)) {
                            currentDirection = moveOnDiagonal();
                        }
                        nCount++;//To make sure we dont break if stuck
                    }
                    foundStartLoc = findStartLoc();
                }

                if(movingBackToStart) {
                    Direction toStartLoc = new Direction(currentLoc, startLoc);
                    if(currentLoc.distanceTo(startLoc) == 0.01){
                        rc.move(toStartLoc, (float)0.01);
                    } else if(rc.canMove(toStartLoc)){
                        rc.move(toStartLoc);
                        currentLoc = rc.getLocation();
                    }
                    if(currentLoc == startLoc)movingBackToStart = false;
                } else {
                    if(rc.getTeamBullets() >= 50 && foundStartLoc) {
                        for (int i = 0; i < treesPlanted.length; i++) {
                            if (!treesPlanted[i]) {
                                treesPlanted[i] = tryPlantTree(i);
                                break;
                            }
                        }
                    }
                }

                if(foundStartLoc) {
                    nearbyTrees = rc.senseNearbyTrees(-1, rc.getTeam());
                    //if(nearbyTrees.length > 0)System.out.println(nearbyTrees.length);
                    minTreeHealth = 50;
                    for (int i = 0; i < nearbyTrees.length; i++) {
                        if (nearbyTrees[i].getHealth() < minTreeHealth) {
                            minTreeHealth = nearbyTrees[i].getHealth();
                            if (rc.canWater(nearbyTrees[i].getID())) {
                                treeID = nearbyTrees[i].getID();
                            }
                        }
                    }
                    if(rc.canWater(treeID)) {
                        rc.water(treeID);
                    }
                }
                Clock.yield();
            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    static boolean tryPlantTree(int treeArrayLoc) throws GameActionException{
        switch(treeArrayLoc) {
            case 0:
                if (rc.canPlantTree(Direction.getWest())) {
                    rc.plantTree(Direction.getWest());
                    return true;
                }
                return false;
            case 1:
                if (!hasMoved && currentLoc == startLoc) {
                    if (rc.canMove(Direction.getSouth())) {
                        rc.move(Direction.getSouth());
                        currentLoc = rc.getLocation();
                        hasMoved = true;
                    }
                } else if(hasMoved){
                    if (rc.canMove(Direction.getSouth())) {
                        rc.move(Direction.getSouth());
                        currentLoc = rc.getLocation();
                        hasMoved = false;
                    }
                } else {
                    if(rc.canMove(Direction.getSouth(), (float)0.01)) {
                        rc.move(Direction.getSouth(), (float)0.01);
                    }
                }
                if (rc.canPlantTree(Direction.getWest())) {
                    rc.plantTree(Direction.getWest());
                    movingBackToStart = true;
                    return true;
                }
                return false;


        }
        return false;
    }

    static boolean findStartLoc() throws GameActionException{
        nearbyTrees = rc.senseNearbyTrees(3);
        nearbyRobots = rc.senseNearbyRobots();
        currentLoc = rc.getLocation();
        if(nearbyTrees.length == 0 && nearbyRobots.length == 0 && rc.onTheMap(currentLoc, 5)){
            startLoc = currentLoc;
            return true;
        }
        return false;
    }

    static int moveOnDiagonal() throws GameActionException{
        nearbyTrees = rc.senseNearbyTrees();
        boolean moveUp = rc.canMove(Direction.getNorth()) && rc.onTheMap(currentLoc.add(Direction.getNorth()), 3);
        boolean moveDown = rc.canMove(Direction.getSouth()) && rc.onTheMap(currentLoc.add(Direction.getSouth()), 3);
        boolean moveLeft = rc.canMove(Direction.getWest()) && rc.onTheMap(currentLoc.add(Direction.getWest()), 3);
        boolean moveRight = rc.canMove(Direction.getEast()) && rc.onTheMap(currentLoc.add(Direction.getEast()), 3);
        System.out.println(moveLeft);
        switch(currentDirection){
            case 1:
                if(moveLeft && !moveDown){
                    return 7;
                } else if(!moveLeft && moveDown){
                    return 3;
                } else {
                    return 5;
                }
            case 3:
                if(moveRight && !moveDown){
                    return 5;
                } else if(!moveRight && moveDown){
                    return 1;
                } else {
                    return 7;
                }
            case 5:
                if(moveRight && !moveUp){
                    return 3;
                } else if(!moveRight && moveUp){
                    return 7;
                } else {
                    return 1;
                }
            case 7:
                if(/*moveLeft && */!moveUp){
                    return 1;
                } else if(!moveLeft/* && moveUp*/){
                    return 5;
                } else {
                    return 3;
                }
            default:
                if(moveLeft && moveUp)return 7;
                if(moveLeft && moveDown)return 1;
                if(moveRight && moveUp)return 5;
                if(moveRight && moveDown)return 3;
                break;
        }
        return 6;
    }

    static Direction[] initDirList() {
        for (int i = 0; i < 8; i ++) {
            float radians = (float)(-Math.PI + 2*Math.PI*((float)i)/8);
            dirList[i] = new Direction(radians);
        }
        return dirList;
    }

    static boolean[] initTP(){
        for(int i = 0; i < treesPlanted.length; i++){
            treesPlanted[i] = false;
        }
        return treesPlanted;
    }

    static void runArchon() throws GameActionException {
        while (true) {

            try {
                Direction dir = dirList[rand.nextInt(7)];
                if (rc.canHireGardener(dir) && rc.getTeamBullets() >= 100) {
                    rc.hireGardener(dir);
                }
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                }

                // Move randomly
                tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius+GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                if(robots.length > 0 && !rc.hasAttacked()) {
                    // Use strike() to hit all nearby robots!
                    rc.strike();
                } else {
                    // No close robots, so search for robots within sight radius
                    robots = rc.senseNearbyRobots(-1,enemy);

                    // If there is a robot, move towards it
                    if(robots.length > 0) {
                        MapLocation myLocation = rc.getLocation();
                        MapLocation enemyLocation = robots[0].getLocation();
                        Direction toEnemy = myLocation.directionTo(enemyLocation);

                        tryMove(toEnemy);
                    } else {
                        // Move Randomly
                        tryMove(randomDirection());
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns a random Direction
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float)Math.random() * 2 * (float)Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir,20,3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir The intended direction of movement
     * @param degreeOffset Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir) && rc.onTheMap(currentLoc.add(dir, 3))) {
            rc.move(dir);
            currentLoc = rc.getLocation();
            return true;
        }

        // Now try a bunch of similar angles
        //boolean moved = false;
        int currentCheck = 1;

        while(currentCheck<=checksPerSide) {
            // Try the offset of the left side
            if(rc.canMove(dir.rotateLeftDegrees(degreeOffset*currentCheck)) && rc.onTheMap(currentLoc.add(dir, 3))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset*currentCheck));
                currentLoc = rc.getLocation();
                return true;
            }
            // Try the offset on the right side
            if(rc.canMove(dir.rotateRightDegrees(degreeOffset*currentCheck)) && rc.onTheMap(currentLoc.add(dir, 3))) {
                rc.move(dir.rotateRightDegrees(degreeOffset*currentCheck));
                currentLoc = rc.getLocation();
                return true;
            }
            // No move performed, try slightly further
            currentCheck++;
        }

        // A move never happened, so return false.
        return false;
    }

    /**
     * A slightly more complicated example function, this returns true if the given bullet is on a collision
     * course with the current robot. Doesn't take into account objects between the bullet and this robot.
     *
     * @param bullet The bullet in question
     * @return True if the line of the bullet's path intersects with this robot's current position.
     */
    static boolean willCollideWithMe(BulletInfo bullet) {
        MapLocation myLocation = rc.getLocation();

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(myLocation);
        float distToRobot = bulletLocation.distanceTo(myLocation);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI/2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float)Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
}
