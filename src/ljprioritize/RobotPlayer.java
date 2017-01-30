/*In the team array 5, and 6 are used for max Scouts and Gardeners*/

package ljprioritize;

import battlecode.common.*;

public strictfp class RobotPlayer {
    static RobotController rc;
    static int GARDENER_CHANNEL = 5;
    static int SCOUT_CHANNEL = 6;
    static int GARDENER_MAX = 5;
    static int SCOUT_MAX = 5;
    static Direction[] dirList = new Direction[8]; // LEFT = 0, DOWN = 2, etc.
    static TreeInfo nearbyTrees[];
    static RobotInfo[] enemies;
    static Team enemy;
    static Team homeTeam;
    static int currentDirection = 6;
    static MapLocation myLocation;
    static boolean movedTwice = true;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;
        myLocation = rc.getLocation();
        enemy = rc.getTeam().opponent();
        enemies = rc.senseNearbyRobots(-1, enemy);
        homeTeam = rc.getTeam();
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
            case TANK:
                runTank();
                break;
            case SCOUT:
                runScout();
                break;
        }
    }

    static void runArchon() throws GameActionException {
        while (true) {
            try {
                Direction dir = randomDirection();
                int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
                rc.broadcast(GARDENER_CHANNEL, 0);
                if (prevNumGard < GARDENER_MAX && rc.canHireGardener(dir)) {
                    rc.hireGardener(dir);
                    rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
                }

                // Move randomly
                tryMove(randomDirection());

                // Broadcast archon's location for other robots on the team to know
                MapLocation myLocation = rc.getLocation();
                rc.broadcast(0, (int) myLocation.x);
                rc.broadcast(1, (int) myLocation.y);
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

    static void runGardener() throws GameActionException {
        /*if(movedTwice && currentDirection != 6 && currentDirection != 2){
            currentDirection = 6;
        }*/
        float minTreeHealth = 50;
        int treeID = 0;
        while (true) {
            try {
                nearbyTrees = rc.senseNearbyTrees();
                Direction dir = randomDirection();
                int prevNumGard = rc.readBroadcast(SCOUT_CHANNEL);
                if (prevNumGard < SCOUT_MAX && rc.canBuildRobot(RobotType.SCOUT, dir)) {
                    rc.buildRobot(RobotType.SCOUT, dir);
                    rc.broadcast(SCOUT_CHANNEL, prevNumGard + 1);
                }// Randomly attempt to build a scout or lumberjack in this direction
                else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && rc.isBuildReady()) {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                }

                for (int i = 0; i < dirList.length; i += 4) {
                    if (enemies.length == 0) {
                        if (rc.canPlantTree(dirList[i])) {
                            rc.plantTree(dirList[i]);
                            break;
                        }
                    } else {
                        Direction enemyDir = dirList[i];
                        //plants tree between gardener and enemy
                        for (int v = 0; v < enemies.length; v++) {
                            enemyDir = new Direction(enemies[v].location.x, enemies[v].location.y);
                            if (rc.canPlantTree(enemyDir)) {
                                rc.plantTree(enemyDir);
                                break;
                            }
                        }
                        //enemyDir.rotateLeftDegrees(180);
                        //tryMove(enemyDir.rotateLeftDegrees(180)); Move opposite enemy (causes errors)
                    }
                }

                /*for (int i = 0; i < nearbyTrees.length; i++) {
                    if (nearbyTrees[i].getHealth() < minTreeHealth) {
                        minTreeHealth = nearbyTrees[i].getHealth();
                        //minTreeArrayLoc = i;
                        if(rc.canWater(nearbyTrees[i].getID())) {
                            treeID = nearbyTrees[i].getID();
                        }
                        System.out.println(treeID);
                    }
                }
                if(rc.canWater(treeID)) {
                    rc.water(treeID);
                }*/
                if (rc.canWater()) {
                    TreeInfo[] nearbyTrees = rc.senseNearbyTrees();
                    for (int i = 0; i < nearbyTrees.length; i++)
                        if (nearbyTrees[i].getTeam() == homeTeam) {
                            if (nearbyTrees[i].getHealth() < GameConstants.BULLET_TREE_MAX_HEALTH - GameConstants.WATER_HEALTH_REGEN_RATE) {
                                if (rc.canWater(nearbyTrees[i].getID())) {
                                    rc.water(nearbyTrees[i].getID());
                                    break;
                                }
                            }
                        }
                }

                tryMove(dirList[currentDirection]);
                if (!rc.hasMoved()) {
                    if (currentDirection == 6) {
                        currentDirection = 2;
                    } else if (currentDirection == 2) {
                        currentDirection = 6;
                    }
                    tryMove(dirList[currentDirection]);
                }/* else if (currentDirection == 4 || currentDirection == 0){
                    movedTwice = true;
                }
                if(!rc.hasMoved()){
                    if(currentDirection == 4){
                        currentDirection = 0;
                    } else {
                        currentDirection = 4;
                    }
                    tryMove(dirList[currentDirection]);
                    if(rc.hasMoved()){
                        movedTwice = false;
                    }
                }*/


                tryDonate();
                Clock.yield();
            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    static Direction[] initDirList() {
        for (int i = 0; i < 8; i++) {
            float radians = (float) (-Math.PI + 2 * Math.PI * ((float) i) / 8);
            dirList[i] = new Direction(radians);
        }
        return dirList;
    }

    static void tryDonate() throws GameActionException {
        if (rc.getTeamBullets() > 100 && rc.getRobotCount() >= 10) {
            float donationAmount = rc.getTeamBullets() - 100;
            rc.donate(donationAmount);
        }
    }

    static void runSoldier() throws GameActionException {
        System.out.println("I'm an soldier!");
        Team enemy = rc.getTeam().opponent();

        int xPos = rc.readBroadcast(0);
        int yPos = rc.readBroadcast(1);


        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();
                TreeInfo[] trees = rc.senseNearbyTrees(-1);

                // See if there are any nearby enemy robots
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                // If there are some...
                if (xPos != 0) {
                    if (rc.canFireSingleShot()) {
                        rc.fireSingleShot(new Direction(xPos, yPos));
                    }
                }
                if (robots.length > 0) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        // ...Then fire a bullet in the direction of the enemy.
                        rc.fireSingleShot(rc.getLocation().directionTo(robots[0].location));
                    }
                } else if (trees.length > 0) {
                    if (rc.canFireSingleShot()) {
                        rc.broadcast(2, (int) trees[0].location.y);
                        rc.broadcast(3, (int) trees[0].location.y);
                        rc.fireSingleShot(rc.getLocation().directionTo(trees[0].location));
                    }
                }

                // Move randomly
                //tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }
    }
    static int moveOnDiagonal() throws GameActionException{
        nearbyTrees = rc.senseNearbyTrees();
        MapLocation currentLoc = rc.getLocation();
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

    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        /* in run scout, added if type == scout or ARCHON
        added moveDiaganol, so at least they wouldn't get stuck together
         added broadcasting for sensed trees, and enemies
         TODO:

        * need to find a way to overwrite broadcasted array - look at comment near it
        **/
        /*TODO:
        one defensive lj per gardener, then become defensive
        * shouldn't be able to change positions
        * have a count/check each round to see if they are alive
        * */
        int nXPos = 0; // xPos of array
        while (true) {
            try {
                myLocation = rc.getLocation();
                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                enemies = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                for (RobotInfo b : enemies) {
                    if (b.getTeam() != homeTeam && rc.canStrike()) {
                        rc.strike();
                        System.out.println("STRIKE");
                        break;
                    }
                }
                enemies = rc.senseNearbyRobots(-1, enemy);

                for (RobotInfo b : enemies) {
                    if (b.getTeam() != homeTeam && !rc.hasMoved()) {
                        nXPos = 400;
                        while (true) {
                            if (rc.readBroadcast(nXPos) == 0) {
                                rc.broadcast(nXPos, Math.round(b.location.x));
                                rc.broadcast(nXPos + 1, Math.round(b.location.y));
                                break;
                            } else {
                                nXPos++;
                                if (nXPos > 499) {
                                    break;
                                }
                            }
                        }
                        tryMove(myLocation.directionTo(b.location));
                        System.out.println("Moving to sensed enemy");
                        break;
                    }
                }
                if (!rc.hasMoved()) {
                    System.out.println("GONNA READ ENEMIES");
                    nXPos = 400; // start at enemy
                    while (true) {
                        if (rc.readBroadcast(nXPos) > 0) {
                            MapLocation enemyBot = new MapLocation(rc.readBroadcast(nXPos), rc.readBroadcast(nXPos + 1));
                            if (!rc.canSenseLocation(enemyBot)) {
                                System.out.println("GO TO THE ENEMY");
                                tryMove(myLocation.directionTo(enemyBot));
                                break;
                            } else {
                                // the robot will never be at the old location again...
                                if (rc.senseRobotAtLocation(enemyBot) != null) {
                                    System.out.println("GO TO THE ENEMY");
                                    tryMove(myLocation.directionTo(enemyBot));
                                    break;
                                } else {
                                    rc.broadcast(nXPos, 0);
                                    rc.broadcast(nXPos + 1, 0);
                                }
                            }
                        } else {
                            nXPos += 2;
                            if (nXPos > 499) {
                                System.out.println("Nothing to read in enemies");
                                break;
                            }
                        }
                    }
                }
                nearbyTrees = rc.senseNearbyTrees();
                for (TreeInfo t : nearbyTrees) {
                    if (rc.canChop(t.getLocation()) && t.team != homeTeam) {
                        rc.chop(t.getLocation());
                        System.out.println("CHOP");
                        break;
                    }
                }
                myLocation = rc.getLocation();
                for (TreeInfo t : nearbyTrees) {
                    if (t.getTeam() != homeTeam) {
                        nXPos = 500;
                        while (true) {
                            if (rc.readBroadcast(nXPos) == 0) {
                                rc.broadcast(nXPos, Math.round(t.location.x));
                                rc.broadcast(nXPos + 1, Math.round(t.location.y));
                                break;
                            } else {
                                nXPos++;
                                if (nXPos > 599) {
                                    break;
                                }
                            }
                        }
                        Direction toTree = myLocation.directionTo(t.location);
                        if (!rc.hasMoved()) {
                            tryMove(toTree);
                        } else {
                            break;
                        }

                    }
                }
                if (!rc.hasMoved()) {
                    System.out.println("GONNA READ TREES");
                    nXPos = 500;
                    while (true) {
                        if (rc.readBroadcast(nXPos) > 0) {
                            MapLocation enemyTree = new MapLocation(rc.readBroadcast(nXPos), rc.readBroadcast(nXPos + 1));
                            if (rc.canSenseLocation(enemyTree)) {
                                if (rc.senseTreeAtLocation(enemyTree) != null) {
                                    System.out.println("GO TO THE TREE");
                                    Direction dirToEnemyTree = new Direction(myLocation, enemyTree);
                                    tryMove(dirToEnemyTree);
                                    break;
                                } else {
                                    rc.broadcast(nXPos, 0);
                                    rc.broadcast(nXPos + 1, 0);
                                }
                            } else {
                                System.out.println("GO TO THE TREE");
                                Direction dirToEnemyTree = new Direction(myLocation, enemyTree);
                                tryMove(dirToEnemyTree);
                                break;
                            }

                        } else {
                            nXPos += 2;
                            if (nXPos > 599) {
                                System.out.println("Nothing to read in trees");
                                break;
                            }
                        }
                    }
                }
                if (!rc.hasMoved()) moveOnDiagonal();

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Lumberjack Exception");
                e.printStackTrace();
            }
        }

    }

    static void runScout() throws GameActionException {
        System.out.println("I'm a scout!");
        Team enemy = rc.getTeam().opponent();
        Team team = rc.getTeam();
        int currentDirection = 0;
        int dir = 0;

        //int xPos = rc.readBroadcast(0);
        //int yPos = rc.readBroadcast(1);
        int nFarWest;
        int nFarEast;
        int nFarSouth;
        int nFarNorth;
        int nTargetTree = 0;
        MapLocation mlLastKnownTree = new MapLocation(1, 1);

        boolean hasReachedWest = false;
        boolean hasReachedEast = false;
        boolean hasReachedNorth = false;
        boolean hasReachedSouth = false;
        //int[] shakenTreeIDs = new int[5];

        // The code you want your robot to perform every round should be in this loop
        while (true) {


            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                MapLocation myLocation = rc.getLocation();
                //TreeInfo[] trees = rc.senseNearbyTrees();
                // See if there are any nearby enemy robots
                nFarWest = rc.readBroadcast(0);
                nFarEast = rc.readBroadcast(1);
                nFarSouth = rc.readBroadcast(2);
                nFarNorth = rc.readBroadcast(3);
                if (nFarWest != 0) {
                    hasReachedWest = true;
                }
                if (nFarEast != 0) {
                    hasReachedEast = true;
                }
                if (nFarSouth != 0) {
                    hasReachedSouth = true;
                }
                if (nFarNorth != 0) {
                    hasReachedNorth = true;
                }
                TreeInfo[] trees = rc.senseNearbyTrees();
                if (nTargetTree == 0) {

                    if (trees.length > 0) {

                        mlLastKnownTree = trees[0].location;
                        for (int i = 0; i < trees.length; i++) {
                            nTargetTree = trees[i].getID();
                            if (trees[i].getTeam() == enemy) {
                                int nXPos = 500; //where the XPos is stored in array
                                while (true) {
                                    if (rc.readBroadcast(nXPos) == 0) {
                                        rc.broadcast(nXPos, Math.round(trees[i].location.x));
                                        rc.broadcast(nXPos + 1, Math.round(trees[i].location.y));
                                        break;
                                    } else {
                                        nXPos++;
                                        if (nXPos > 600) {
                                            break;
                                        }
                                    }
                                }
                            }
                            if (trees[i].getContainedBullets() > 0) {
                                break;
                            } else {
                                nTargetTree = 0;
                            }
                        }
                    }
                }
                if (nTargetTree != 0) {
                    Direction toTree = myLocation.directionTo(rc.senseTree(nTargetTree).location);
                    tryMove(toTree);
                    System.out.println("CONTINUING");
                    if (rc.canShake(nTargetTree)) {

                        rc.shake(nTargetTree);
                        System.out.println("SHAKING");
                        nTargetTree = 0;
                    }
                }

                if (!rc.hasMoved()) {
                    if (!hasReachedWest) {
                        MapLocation prevLoc = myLocation;
                        tryMove(Direction.getWest());
                        myLocation = rc.getLocation();
                        System.out.println("DIRECTIONS");
                        if (myLocation == prevLoc) {
                            hasReachedWest = true;
                            rc.broadcast(0, Math.round(myLocation.x));
                        }
                    } else if (!hasReachedEast) {
                        MapLocation prevLoc = myLocation;
                        tryMove(Direction.getEast());
                        myLocation = rc.getLocation();
                        System.out.println("DIRECTIONS");
                        if (myLocation == prevLoc) {
                            hasReachedEast = true;
                            rc.broadcast(1, Math.round(myLocation.x));
                        }
                    } else if (!hasReachedSouth) {
                        MapLocation prevLoc = myLocation;
                        tryMove(Direction.getSouth());
                        myLocation = rc.getLocation();
                        System.out.println("DIRECTIONS");
                        if (myLocation == prevLoc) {
                            hasReachedSouth = true;
                            rc.broadcast(2, Math.round(myLocation.y));
                        }
                    } else if (!hasReachedNorth) {
                        MapLocation prevLoc = myLocation;
                        tryMove(Direction.getNorth());
                        myLocation = rc.getLocation();
                        System.out.println("DIRECTIONS");
                        if (myLocation == prevLoc) {
                            hasReachedNorth = true;
                            rc.broadcast(3, Math.round(myLocation.y));
                        }
                    } else {
                        RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                        int nTargetRobot = 0;
                        int XPos = 400;
                        for (int i = 0; i < robots.length; i++) {
                            if (robots[i].getType() == RobotType.GARDENER || robots[i].getType() == RobotType.ARCHON) {
                                nTargetRobot = robots[i].getID();
                                while (true) {
                                    if (rc.readBroadcast(XPos) == 0) {
                                        rc.broadcast(XPos, Math.round(robots[i].location.x));
                                        rc.broadcast(XPos + 1, Math.round(robots[i].location.y));
                                        break;
                                    } else {
                                        XPos += 2;
                                        if (XPos > 499) {
                                            break;
                                        }
                                    }

                                }
                                MapLocation enemyBot = robots[i].location;
                                Direction dirToEnemyBot = new Direction(myLocation, enemyBot);
                                tryMove(dirToEnemyBot);
                                if (rc.canFireSingleShot()) {
                                    rc.fireSingleShot(dirToEnemyBot);
                                }
                                break;
                            }
                        }
                        if (nTargetRobot == 0) {
                            XPos = 400;
                            while (true) {
                                if (rc.readBroadcast(XPos) != 0) {
                                    MapLocation enemyBot = new MapLocation(rc.readBroadcast(XPos), rc.readBroadcast(XPos + 1));
                                    Direction dirToEnemyBot = new Direction(myLocation, enemyBot);
                                    if (!rc.hasMoved()) tryMove(dirToEnemyBot);
                                    nTargetRobot = rc.readBroadcast(XPos);
                                } else {
                                    XPos++;
                                    if (XPos > 499) {
                                        break;
                                    }
                                }
                            }
                        }

                        if (!rc.hasMoved()) {

                            if (rc.canMove(dirList[dir])) {
                                tryMove(dirList[dir]);
                            } else {
                                dir += 2;
                                if (dir > 7) {
                                    dir = 0;
                                }
                            }


                        }


                    }
                }

                // Move randomly
                //tryMove(randomDirection());

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("SCOuT Exception!?");
                e.printStackTrace();
            }
        }

    }

    static void runTank() throws GameActionException {

    }

    public static boolean isInTree(MapLocation mlMyLocation, MapLocation mlTree) {
        if (mlMyLocation.x - 2 <= mlTree.x && mlMyLocation.x + 2 >= mlTree.x && mlMyLocation.y - 2 <= mlTree.y && mlMyLocation.y + 2 >= mlTree.y) {
            return true;
        } else {
            return false;
        }
    }

    public static void wander() throws GameActionException {
        try {
            Direction dir = randomDirection();
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a random Direction
     *
     * @return a random Direction
     */
    static Direction randomDirection() {
        return new Direction((float) Math.random() * 2 * (float) Math.PI);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles directly in the path.
     *
     * @param dir The intended direction of movement
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir) throws GameActionException {
        return tryMove(dir, 20, 3);
    }

    /**
     * Attempts to move in a given direction, while avoiding small obstacles direction in the path.
     *
     * @param dir           The intended direction of movement
     * @param degreeOffset  Spacing between checked directions (degrees)
     * @param checksPerSide Number of extra directions checked on each side, if intended direction was unavailable
     * @return true if a move was performed
     * @throws GameActionException
     */
    static boolean tryMove(Direction dir, float degreeOffset, int checksPerSide) throws GameActionException {

        // First, try intended direction
        if (rc.canMove(dir)) {
            rc.move(dir);
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while (currentCheck <= checksPerSide) {
            // Try the offset of the left side
            if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
                return true;
            }
            // Try the offset on the right side
            if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
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
        if (Math.abs(theta) > Math.PI / 2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from myLocation and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }

    static boolean trySidestep(BulletInfo bullet) throws GameActionException {

        Direction towards = bullet.getDir();
        MapLocation leftGoal = rc.getLocation().add(towards.rotateLeftDegrees(90), rc.getType().bodyRadius);
        MapLocation rightGoal = rc.getLocation().add(towards.rotateRightDegrees(90), rc.getType().bodyRadius);

        return (tryMove(towards.rotateRightDegrees(90)) || tryMove(towards.rotateLeftDegrees(90)));
    }

    static void dodge() throws GameActionException {
        BulletInfo[] bullets = rc.senseNearbyBullets();
        for (BulletInfo bi : bullets) {
            if (willCollideWithMe(bi)) {
                trySidestep(bi);
            }
        }

    }

    public static void tryToBuild(RobotType type) throws GameActionException {
        for (int i = 0; i < 8; i++) {
            if (rc.canBuildRobot(type, dirList[i])) {
                rc.buildRobot(type, dirList[i]);
                break;
            }
        }
    }

    public static void tryToPlant() throws GameActionException {
        for (int i = 0; i < 8; i++) {
            if (rc.canPlantTree(dirList[i])) {
                rc.plantTree(dirList[i]);
                break;
            }
        }
    }

}


