/*In the team array 5, and 6 are used for max Scouts and Gardeners*/
/*
I need to get rid of some code now:
In 1.2.2:
Robots/Trees/Bullets from senseNearbyRobots/Trees/Bullets() are now returned in order of increasing distance from the specified center point.
Server:
canWater() now returns false for neutral trees.
*/
package abhi;

import battlecode.common.*;

import java.util.ArrayList;

public strictfp class RobotPlayer {
    static RobotController rc;

    static Direction[] dirList = new Direction[8];
    // Keep broadcast channels
    static int GARDENER_CHANNEL = 5;
    static int SCOUT_CHANNEL = 6;

    // Keep important numbers here
    static int GARDENER_MAX = 5;
    static int SCOUT_MAX = 5;

    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        dirList = initDirList();

        // Here, we've separated the controls into a different method for each RobotType.
        // You can add the missing ones or rewrite this into your own control structure.
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

    public static Direction[] initDirList() {
        for (int i = 0; i < 8; i++) {
            float radians = (float) (-Math.PI + 2 * Math.PI * ((float) i) / 8);
            dirList[i] = new Direction(radians);
        }
        return dirList;
    }
    static void runArchon() throws GameActionException {
        System.out.println("I'm an archon!");

        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {

                // Generate a random direction
                Direction dir = randomDirection();

                int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
                rc.broadcast(GARDENER_CHANNEL, 0);
                if (prevNumGard < GARDENER_MAX && rc.canHireGardener(dir)) {
                    rc.hireGardener(dir);
                    rc.broadcast(GARDENER_CHANNEL, 1);
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
        System.out.println("I'm a gardener!");

        Team enemy = rc.getTeam().opponent();

        Team us = rc.getTeam();
        // The code you want your robot to perform every round should be in this loop
        while (true) {

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                int prev = rc.readBroadcast(GARDENER_CHANNEL);
                rc.broadcast(GARDENER_CHANNEL, prev + 1);
                // Listen for home archon's location
                int xPos = rc.readBroadcast(0);
                int yPos = rc.readBroadcast(1);
                MapLocation archonLoc = new MapLocation(xPos, yPos);
                TreeInfo[] trees = rc.senseNearbyTrees();

                // Generate a random direction
                Direction dir = randomDirection();

                int prevNumGard = rc.readBroadcast(SCOUT_CHANNEL);
                if (prevNumGard < SCOUT_MAX && rc.canBuildRobot(RobotType.SCOUT, dir)) {
                    rc.buildRobot(RobotType.SCOUT, dir);
                    rc.broadcast(SCOUT_CHANNEL, prevNumGard + 1);
                }// Randomly attempt to build a scout or lumberjack in this direction
                else if (rc.canBuildRobot(RobotType.LUMBERJACK, dir) && rc.isBuildReady()) {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                }
                dir = randomDirection();
                if (rc.canPlantTree(dir)) {
                    rc.plantTree(dir);
                }
                for (TreeInfo t : trees) {
                    if (rc.canWater(t.getID()) && t.getTeam() == us) {
                        rc.water(t.getID());
                        System.out.println("WATER");
                    }
                }

                // Move randomly
                tryMove(randomDirection());

                while (rc.getTeamBullets() >= 500) {
                    rc.donate(10);
                }
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
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

    static void runLumberjack() throws GameActionException {
        System.out.println("I'm a lumberjack!");
        Team enemy = rc.getTeam().opponent();
        Team us = rc.getTeam();

        // The code you want your robot to perform every round should be in this loop
        while (true) {
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                TreeInfo[] trees = rc.senseNearbyTrees();
                for (TreeInfo t : trees) {
                    if (rc.canChop(t.getLocation()) && t.team != us) {
                        rc.chop(t.getLocation());
                        System.out.println("CHOP");
                        break;
                    }
                }
                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] robots = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                for (RobotInfo b : robots) {
                    if (b.getTeam() != us && rc.canStrike()) {
                        rc.strike();
                        System.out.println("STRIKE");/*
                      Direction chase = rc.getLocation().directionTo(b.getLocation());
                      if (rc.canMove(chase)) {
                          rc.move(chase);
                          System.out.println("CHASE");
                      }*/
                        break;
                    }
                }
                if (!rc.hasAttacked() && trees.length != 0) {
                    // for the closest tree
                    MapLocation myLocation = rc.getLocation();
                    MapLocation minTreelocation = null;

                    float distance = 0;
                    for (TreeInfo t : trees) {
                        if (t.getTeam() != us) {
                            MapLocation treeLocation = t.getLocation();
                            if (myLocation.distanceTo(treeLocation) < distance && t.getTeam() != us) {
                                minTreelocation = treeLocation;
                                distance = myLocation.distanceTo(minTreelocation);
                            }
                        }
                    }
                    if (minTreelocation != null) {
                        Direction toTree = myLocation.directionTo(minTreelocation);
                        tryMove(toTree);
                        System.out.println("MOVE");
                    } else {
                        System.out.println("GONNA READ BROADCAST");
                        int XPos = 500;
                        while (true) {
                            if (rc.readBroadcast(XPos) > 0) {
                                MapLocation enemyTree = new MapLocation(rc.readBroadcast(XPos), rc.readBroadcast(XPos + 1));
                                System.out.println("GO TO THE ENEMY");
                                Direction dirToEnemyTree = new Direction(myLocation, enemyTree);
                                tryMove(dirToEnemyTree);
                                break;
                            } else {
                                XPos += 2;

                                if (XPos > 600) {

                                    break;
                                }
                            }
                        }
                    }

                } else {
                    MapLocation myLocation = rc.getLocation();
                    System.out.println("GONNA READ BROADCAST");
                    int XPos = 500;
                    while (true) {
                        if (rc.readBroadcast(XPos) > 0) {
                            MapLocation enemyTree = new MapLocation(rc.readBroadcast(XPos), rc.readBroadcast(XPos + 1));
                            if (rc.canSenseLocation(enemyTree)) {
                                if (rc.senseTreeAtLocation(enemyTree) != null) {
                                    System.out.println("GO TO THE ENEMY");
                                    Direction dirToEnemyTree = new Direction(myLocation, enemyTree);
                                    tryMove(dirToEnemyTree);
                                    break;
                                } else {
                                    rc.broadcast(XPos, 0);
                                    rc.broadcast(XPos + 1, 0);
                                }
                            } else {
                                System.out.println("GO TO THE ENEMY");
                                Direction dirToEnemyTree = new Direction(myLocation, enemyTree);
                                tryMove(dirToEnemyTree);
                                break;
                            }

                        } else {
                            XPos += 2;
                            if (XPos > 600) {
                                System.out.println("NOTHING TO READ HERE");
                                break;
                            }
                        }
                    }
                    //System.out.println("WANDER");
                }

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
        int nFarWest = rc.readBroadcast(0);
        int nFarEast = rc.readBroadcast(1);
        int nFarSouth = rc.readBroadcast(2);
        int nFarNorth = rc.readBroadcast(3);
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
                            if (robots[i].getType() == RobotType.GARDENER) {
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
                                    tryMove(dirToEnemyBot);
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
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }

    }

    static void runTank() throws GameActionException {

    }

    public static void wander() throws GameActionException {
        try {
            Direction dir = randomDirection();
            if (rc.canMove(dir)) {
                rc.move(dir);
                System.out.println("WANDER");
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
            System.out.println("MOVE");
            return true;
        }

        // Now try a bunch of similar angles
        boolean moved = false;
        int currentCheck = 1;

        while (currentCheck <= checksPerSide) {
            // Try the offset of the left side
            if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
                System.out.println("MOVE");
                return true;
            }
            // Try the offset on the right side
            if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck))) {
                rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
                System.out.println("MOVE");
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
}
