/*In the team array 5, and 6 and 7 are used for max Scouts and Gardeners, 987 is the Archon thing with broadcasting 0 for counting*/

package lgsstrategytest3;

import battlecode.common.*;

import java.util.ArrayList;
import java.util.Random;

public strictfp class RobotPlayer {
    static RobotController rc;
    static Random rand = new Random();
    static boolean[] treesPlanted = new boolean[7];
    static boolean plantingComplete = false;
    static boolean hasInitializedTP = false;
    static boolean hasMoved = false;
    static boolean movingBackToStart = false;
    static boolean foundStartLoc = false;
    static boolean isCountingArchon = false;
    static Direction[] dirList = new Direction[8]; // LEFT = 0, DOWN = 2, etc.
    static TreeInfo nearbyTrees[];
    static RobotInfo nearbyRobots[], nearbyEnemies[];
    static MapLocation startLoc;
    static MapLocation currentLoc;
    static int currentDirection = 1;
    static int prevNumGard;
    static int prevNumScout;
    static int GARDENER_CHANNEL = 5;
    static int SCOUT_CHANNEL = 6;
    static int GARDENER_CHANNEL_PREV = 7;
    static int GARDENER_MAX = 8;
    static int SCOUT_MAX = 10;
    static float minTreeHealth;
    static RobotInfo[] enemies;
    static Team enemy;
    static Team homeTeam;
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    public static void run(RobotController rc) throws GameActionException {

        RobotPlayer.rc = rc;
        if (!hasInitializedTP) {
            hasInitializedTP = true;
            treesPlanted = initTP();
        }
        currentLoc = rc.getLocation();
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
        MapLocation[] initialEnemyArchons = rc.getInitialArchonLocations(enemy);
        for (MapLocation l : initialEnemyArchons) {
            int nXPos = 400;
            while (true) {
                if (rc.readBroadcast(nXPos) == 0) {
                    rc.broadcast(nXPos, Math.round(l.x));
                    rc.broadcast(nXPos + 1, Math.round(l.y));
                    //System.out.println("Broadcasting Initial Enemy Archon Locations");
                    //System.out.println(l.x + ", " + l.y);
                    break;
                } else {
                    nXPos++;
                    if (nXPos > 499) {
                        break;
                    }
                }
            }
        }
        while (true) {
            try {
                dodge();
                Direction dir = dirList[rand.nextInt(7)];
                prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
                if (prevNumGard < GARDENER_MAX && rc.canHireGardener(dir) && rc.getTeamBullets() >= 100) {
                    rc.hireGardener(dir);
                    rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
                }
                if(rc.readBroadcast(987) == 0){
                    rc.broadcast(987, 1);
                    if(rc.getHealth() > 20) {
                        isCountingArchon = true;
                    }
                }
                if(isCountingArchon) {
                    rc.broadcast(GARDENER_CHANNEL_PREV, prevNumGard);
                    rc.broadcast(GARDENER_CHANNEL, 0);
                    if(rc.getHealth() <= 20){
                        isCountingArchon = false;
                    }
                }

                //tryMove(randomDirection());
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Archon Exception");
                e.printStackTrace();
            }
        }
    }

    static void runGardener() throws GameActionException {
        int treeID = 0;
        while (true) {
            try {
                prevNumScout = rc.readBroadcast(SCOUT_CHANNEL);
                prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
                rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
                dodge();
                int nCount = 0;
                if (!foundStartLoc) {
                    while (!rc.hasMoved() && nCount != 4) {
                        if (!tryMove(dirList[currentDirection], 5, 2)) {
                            currentDirection = moveOnDiagonal();
                        }
                        nCount++;//To make sure we dont break if stuck
                    }
                    foundStartLoc = findStartLoc();
                }
                if (movingBackToStart) {
                    //System.out.println(startLoc.y + " " + currentLoc.y);
                    Direction toStartLoc = new Direction(currentLoc, startLoc);
                    float distanceToStart = currentLoc.distanceTo(startLoc);
                    if (currentLoc.distanceTo(startLoc) <= 1 && rc.canMove(toStartLoc, distanceToStart)) {
                        rc.move(toStartLoc, distanceToStart);
                        currentLoc = rc.getLocation();
                    } else if (rc.canMove(toStartLoc)) {
                        rc.move(toStartLoc);
                        currentLoc = rc.getLocation();
                    }
                    if (currentLoc.x <= startLoc.x + 0.01 && currentLoc.x >= startLoc.x - 0.01) {
                        if (currentLoc.y <= startLoc.y + 0.01 && currentLoc.y >= startLoc.y - 0.01) {
                            movingBackToStart = false;
                            startLoc = currentLoc;
                        }
                    }
                } else {
                    nearbyEnemies = rc.senseNearbyRobots(4, enemy);
                    if(nearbyEnemies.length > 0){
                        plantingComplete = false;
                        foundStartLoc = false;
                        initTP();
                    }
                    if (foundStartLoc) {

                        if (!plantingComplete && rc.getTeamBullets() >= 50) {
                            for (int i = 0; i < treesPlanted.length; i++) {
                                if (!treesPlanted[i]) {
                                    treesPlanted[i] = tryPlantTree(i);
                                    break;
                                }
                            }
                        } else {

                            if (prevNumScout < SCOUT_MAX && rc.canBuildRobot(RobotType.SCOUT, dirList[6]) && rc.getTeamBullets() >= 80) {
                                rc.buildRobot(RobotType.SCOUT, dirList[6]);
                                rc.broadcast(SCOUT_CHANNEL, prevNumScout + 1);
                            }
                            else if (rc.canBuildRobot(RobotType.LUMBERJACK, dirList[6]) && rc.isBuildReady()) {
                                rc.buildRobot(RobotType.LUMBERJACK, dirList[6]);
                            }
                        }
                    }
                }

                if (foundStartLoc) {
                    nearbyTrees = rc.senseNearbyTrees(-1, rc.getTeam());
                    minTreeHealth = 50;
                    for (int i = 0; i < nearbyTrees.length; i++) {
                        if (nearbyTrees[i].getHealth() < minTreeHealth) {
                            minTreeHealth = nearbyTrees[i].getHealth();
                            if (rc.canWater(nearbyTrees[i].getID())) {
                                treeID = nearbyTrees[i].getID();
                            }
                        }
                    }
                    if (rc.canWater(treeID)) {
                        rc.water(treeID);
                    }
                }
                if(prevNumGard == rc.readBroadcast(GARDENER_CHANNEL_PREV)){
                    rc.broadcast(SCOUT_CHANNEL, 0);
                }
                tryDonate();
                Clock.yield();
            } catch (Exception e) {
                System.out.println("Gardener Exception");
                e.printStackTrace();
            }
        }
    }

    static boolean[] initTP() {
        for (int i = 0; i < treesPlanted.length; i++) {
            treesPlanted[i] = false;
        }
        return treesPlanted;
    }

    static Direction[] initDirList() {
        for (int i = 0; i < 8; i ++) {
            float radians = (float)(-Math.PI + 2*Math.PI*((float)i)/8);
            dirList[i] = new Direction(radians);
        }
        return dirList;
    }

    static void tryDonate() throws GameActionException{
        if(rc.getTeamBullets() >= 10000.42){
            rc.donate((float)(rc.getTeamBullets() - 0.42));
        }
    }

    static void runSoldier() throws GameActionException {
        while (true) {
            try {
                currentLoc = rc.getLocation();
                nearbyTrees = rc.senseNearbyTrees(-1);
                enemies = rc.senseNearbyRobots(-1, enemy);

                for (RobotInfo enemy : enemies) {
                    // And we have enough bullets, and haven't attacked yet this turn...
                    if (rc.canFireSingleShot()) {
                        rc.fireSingleShot(currentLoc.directionTo(enemy.location));
                    } else if (!rc.hasMoved()) {
                        tryMove(currentLoc.directionTo(enemy.location));
                        break;
                    }
                }
                for (TreeInfo t : nearbyTrees) {
                    if (t.getTeam() != homeTeam) {
                        if (rc.canFireSingleShot()) {
                            rc.fireSingleShot(currentLoc.directionTo(t.location));
                        } else if (!rc.hasMoved()) {
                            tryMove(currentLoc.directionTo(t.location));
                            break;
                        }
                    }
                }
                if (!rc.hasMoved()) {
                    // this is for enemies
                    int XPos = 400;
                    while (true) {
                        if (rc.readBroadcast(XPos) > 0) {
                            MapLocation enemyBot = new MapLocation(rc.readBroadcast(XPos), rc.readBroadcast(XPos + 1));
                            if (rc.canSenseLocation(enemyBot)) {
                                if (rc.senseRobotAtLocation(enemyBot) != null) {
                                    Direction dirToEnemyBot = new Direction(currentLoc, enemyBot);
                                    tryMove(dirToEnemyBot);
                                    break;
                                } else {
                                    rc.broadcast(XPos, 0);
                                    rc.broadcast(XPos + 1, 0);
                                }
                            } else {
                                Direction dirToEnemyBot = new Direction(currentLoc, enemyBot);
                                tryMove(dirToEnemyBot);
                                break;
                            }

                        } else {
                            XPos += 2;
                            if (XPos > 500) {
                                if(!tryMove(dirList[currentDirection])) {
                                    currentDirection = moveOnDiagonal();
                                }
                                break;
                            }
                        }
                    }
                }
                if (!rc.hasMoved()) {
                    // this is for trees
                    int XPos = 500;
                    while (true) {
                        if (rc.readBroadcast(XPos) > 0) {
                            MapLocation enemyTree = new MapLocation(rc.readBroadcast(XPos), rc.readBroadcast(XPos + 1));
                            if (rc.canSenseLocation(enemyTree)) {
                                if (rc.senseTreeAtLocation(enemyTree) != null) {
                                    Direction dirToEnemyTree = new Direction(currentLoc, enemyTree);
                                    tryMove(dirToEnemyTree);
                                    break;
                                } else {
                                    rc.broadcast(XPos, 0);
                                    rc.broadcast(XPos + 1, 0);
                                }
                            } else {
                                Direction dirToEnemyTree = new Direction(currentLoc, enemyTree);
                                tryMove(dirToEnemyTree);
                                break;
                            }

                        } else {
                            XPos += 2;
                            if (XPos > 600) {
                                if(!tryMove(dirList[currentDirection])) {
                                    currentDirection = moveOnDiagonal();
                                }
                                break;
                            }
                        }
                    }
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Soldier Exception");
                e.printStackTrace();
            }
        }

    }



    static void runLumberjack() throws GameActionException {
        //System.out.println("I'm a lumberjack!");
   /* in run scout, I added if type == scout || ARCHON when they add to enemy array
   Used moveDiagonal(), so at least they wouldn't get stuck together
    added broadcasting for sensed trees, and enemies
    TODO:

   * need to find a way to overwrite broadcasted array - look at comment near it
   **/

   /*Another TODO: (WIP)
   one defensive lj per gardener, then become defensive
   * shouldn't be able to change positions
   * have a count/check each round to see if they are alive
   * */
        int nXPos = 0; // xPos of array
        while (true) {
            try {
                currentLoc = rc.getLocation();
                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                enemies = rc.senseNearbyRobots(RobotType.LUMBERJACK.bodyRadius + GameConstants.LUMBERJACK_STRIKE_RADIUS, enemy);

                for (RobotInfo b : enemies) {
                    if (b.getTeam() != homeTeam && rc.canStrike()) {
                        rc.strike();
                        //System.out.println("STRIKE");
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
                        tryMove(currentLoc.directionTo(b.location));
                        //System.out.println("Moving to sensed enemy");
                        break;
                    }
                }
                if (!rc.hasMoved()) {
                    //System.out.println("GONNA READ ENEMIES");
                    nXPos = 400; // start at enemy
                    while (true) {
                        if (rc.readBroadcast(nXPos) > 0) {
                            MapLocation enemyBot = new MapLocation(rc.readBroadcast(nXPos), rc.readBroadcast(nXPos + 1));
                            if (!rc.canSenseLocation(enemyBot)) {
                                //System.out.println("GO TO THE ENEMY");
                                tryMove(currentLoc.directionTo(enemyBot));
                                break;
                            } else {
                                // the robot will never be at the old location again...
                                if (rc.senseRobotAtLocation(enemyBot) != null) {
                                    //System.out.println("GO TO THE ENEMY");
                                    tryMove(currentLoc.directionTo(enemyBot));
                                    break;
                                } else {
                                    rc.broadcast(nXPos, 0);
                                    rc.broadcast(nXPos + 1, 0);
                                }
                            }
                        } else {
                            nXPos += 2;
                            if (nXPos > 499) {
                                //System.out.println("Nothing to read in enemies");
                                break;
                            }
                        }
                    }
                }
                nearbyTrees = rc.senseNearbyTrees();
                for (TreeInfo t : nearbyTrees) {
                    if (rc.canChop(t.getLocation()) && t.team != homeTeam) {
                        rc.chop(t.getLocation());
                        //System.out.println("CHOP");
                        break;
                    }
                }
                currentLoc = rc.getLocation();
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
                        Direction toTree = currentLoc.directionTo(t.location);
                        if (!rc.hasMoved()) {
                            tryMove(toTree);
                        } else {
                            break;
                        }

                    }
                }
                if (!rc.hasMoved()) {
                    //System.out.println("GONNA READ TREES");
                    nXPos = 500;
                    while (true) {
                        if (rc.readBroadcast(nXPos) > 0) {
                            MapLocation enemyTree = new MapLocation(rc.readBroadcast(nXPos), rc.readBroadcast(nXPos + 1));
                            if (rc.canSenseLocation(enemyTree)) {
                                if (rc.senseTreeAtLocation(enemyTree) != null) {
                                    //System.out.println("GO TO THE TREE");
                                    Direction dirToEnemyTree = new Direction(currentLoc, enemyTree);
                                    tryMove(dirToEnemyTree, 20, 6);
                                    break;
                                } else {
                                    rc.broadcast(nXPos, 0);
                                    rc.broadcast(nXPos + 1, 0);
                                }
                            } else {
                                //System.out.println("GO TO THE TREE");
                                Direction dirToEnemyTree = new Direction(currentLoc, enemyTree);
                                tryMove(dirToEnemyTree, 20, 6);
                                break;
                            }

                        } else {
                            nXPos += 2;
                            if (nXPos > 599) {
                                //System.out.println("Nothing to read in trees");
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

    static boolean tryPlantTree(int treeArrayLoc) throws GameActionException {
        switch (treeArrayLoc) {
            //LEAVING OUT UP SO WE CAN PLACE TROOPS
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
                } else if (hasMoved) {
                    if (rc.canMove(Direction.getSouth())) {
                        rc.move(Direction.getSouth());
                        currentLoc = rc.getLocation();
                        hasMoved = false;
                    }
                } else {
                    if (rc.canMove(Direction.getSouth(), (float) 0.001)) {
                        rc.move(Direction.getSouth(), (float) 0.001);
                    }
                }
                if (rc.canPlantTree(Direction.getWest())) {
                    rc.plantTree(Direction.getWest());
                    movingBackToStart = true;
                    return true;
                }
                return false;
            case 2:
                if (rc.canPlantTree(Direction.getSouth())) {
                    rc.plantTree(Direction.getSouth());
                    return true;
                }
                return false;
            case 3:
                if (!hasMoved && currentLoc == startLoc) {
                    if (rc.canMove(Direction.getEast())) {
                        rc.move(Direction.getEast());
                        currentLoc = rc.getLocation();
                        hasMoved = true;
                    }
                } else if(hasMoved){
                    if (rc.canMove(Direction.getEast())) {
                        rc.move(Direction.getEast());
                        currentLoc = rc.getLocation();
                        hasMoved = false;
                    }
                } else {
                    if(rc.canMove(Direction.getEast(), (float)0.001)) {
                        rc.move(Direction.getEast(), (float)0.001);
                    }
                }
                if (rc.canPlantTree(Direction.getSouth())) {
                    rc.plantTree(Direction.getSouth());
                    movingBackToStart = true;
                    return true;
                }
                return false;
            case 4:
                if (rc.canPlantTree(Direction.getEast())) {
                    rc.plantTree(Direction.getEast());
                    return true;
                }
                return false;
            case 5:
                if (!hasMoved && currentLoc == startLoc) {
                    if (rc.canMove(Direction.getNorth())) {
                        rc.move(Direction.getNorth());
                        currentLoc = rc.getLocation();
                        hasMoved = true;
                    }
                } else if (hasMoved) {
                    if (rc.canMove(Direction.getNorth())) {
                        rc.move(Direction.getNorth());
                        currentLoc = rc.getLocation();
                        hasMoved = false;
                    }
                } else {
                    if (rc.canMove(Direction.getNorth(), (float) 0.001)) {
                        rc.move(Direction.getNorth(), (float) 0.001);
                    }
                }
                if (rc.canPlantTree(Direction.getEast())) {
                    rc.plantTree(Direction.getEast());
                    movingBackToStart = true;
                    return true;
                }
                return false;
            case 6:
                if (!hasMoved && currentLoc == startLoc) {
                    if (rc.canMove(Direction.getNorth())) {
                        rc.move(Direction.getNorth());
                        currentLoc = rc.getLocation();
                        hasMoved = true;
                    }
                } else if (hasMoved) {
                    if (rc.canMove(Direction.getNorth())) {
                        rc.move(Direction.getNorth());
                        currentLoc = rc.getLocation();
                        hasMoved = false;
                    }
                } else {
                    if (rc.canMove(Direction.getNorth(), (float) 0.001)) {
                        rc.move(Direction.getNorth(), (float) 0.001);
                    }
                }
                if (rc.canPlantTree(Direction.getWest())) {
                    rc.plantTree(Direction.getWest());
                    plantingComplete = true;
                    movingBackToStart = true;
                    return true;
                }
                return false;
        }
        return false;
    }

    static boolean findStartLoc() throws GameActionException {
        nearbyTrees = rc.senseNearbyTrees((float)1.1);
        nearbyRobots = rc.senseNearbyRobots();
        currentLoc = rc.getLocation();
        if (nearbyTrees.length == 0 && nearbyRobots.length == 0 && rc.onTheMap(currentLoc, (float)3.1)) {
            startLoc = currentLoc;
            return true;
        }
        return false;
    }

    static int moveOnDiagonal() throws GameActionException {
        nearbyTrees = rc.senseNearbyTrees();
        boolean moveUp = rc.canMove(Direction.getNorth()) && rc.onTheMap(currentLoc.add(Direction.getNorth()), 3);
        boolean moveDown = rc.canMove(Direction.getSouth()) && rc.onTheMap(currentLoc.add(Direction.getSouth()), 3);
        boolean moveLeft = rc.canMove(Direction.getWest()) && rc.onTheMap(currentLoc.add(Direction.getWest()), 3);
        boolean moveRight = rc.canMove(Direction.getEast()) && rc.onTheMap(currentLoc.add(Direction.getEast()), 3);
        switch (currentDirection) {
            case 1:
                if (moveLeft && !moveDown) {
                    return 7;
                } else if (!moveLeft && moveDown) {
                    return 3;
                } else {
                    return 5;
                }
            case 3:
                if (moveRight && !moveDown) {
                    return 5;
                } else if (!moveRight && moveDown) {
                    return 1;
                } else {
                    return 7;
                }
            case 5:
                if (moveRight && !moveUp) {
                    return 3;
                } else if (!moveRight && moveUp) {
                    return 7;
                } else {
                    return 1;
                }
            case 7:
                if (moveLeft && !moveUp) {
                    return 1;
                } else if (!moveLeft && moveUp) {
                    return 5;
                } else {
                    return 3;
                }
            default:
                if (moveLeft && moveUp) return 7;
                if (moveLeft && moveDown) return 1;
                if (moveRight && moveUp) return 5;
                if (moveRight && moveDown) return 3;
                break;
        }
        return 6;
    }

    static void runScout() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        int nTargetRobot;
        int nTargetGardener;

        int treeNearEnemy;
        MapLocation mlTargetRobotLoc;
        MapLocation mlTargetGardenLoc;
        MapLocation mlTreeNearEnemy;

        //int xPos = rc.readBroadcast(0);
        //int yPos = rc.readBroadcast(1);
        int nTargetTree = 0;
        MapLocation mlLastKnownTree = new MapLocation(1, 1);
        MapLocation mlTargetTree = null;

        boolean bBattleMode;
        //int[] shakenTreeIDs = new int[5];

        // The code you want your robot to perform every round should be in this loop
        while (true) {


            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                prevNumScout = rc.readBroadcast(SCOUT_CHANNEL);
                System.out.println(prevNumScout);
                rc.broadcast(SCOUT_CHANNEL, prevNumScout + 1);
                nTargetRobot = 0;
                nTargetGardener = 0;
                treeNearEnemy = 0;
                mlTargetRobotLoc = null;
                mlTargetGardenLoc = null;
                mlTreeNearEnemy = null;
                dodge();
                TreeInfo nearbyTrees[] = rc.senseNearbyTrees();

                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);
                int XPos = 400;
                if (robots.length > 0) {
                    bBattleMode = true;
                    for (int i = 0; i < robots.length; i++) {
                        if (robots[i].getType() == RobotType.GARDENER || robots[i].getType() == RobotType.ARCHON) {
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
                        }
                        if (nTargetGardener == 0) {
                            if (robots[i].getType() == RobotType.GARDENER) {
                                nTargetGardener = robots[i].getID();
                                mlTargetGardenLoc = robots[i].location;
                                break;
                            }
                        }
                        if (nTargetRobot == 0) {
                            nTargetRobot = robots[i].getID();
                            mlTargetRobotLoc = robots[i].location;
                        }
                    }
                    if (nTargetGardener != 0) {
                        if (mlTargetGardenLoc != null) {
                            TreeInfo treesNearEnemy[] = rc.senseNearbyTrees(mlTargetGardenLoc, RobotType.SCOUT.sensorRadius, enemy);
                            for (int i = 0; i < treesNearEnemy.length; i++) {
                                if (rc.canSenseTree(treesNearEnemy[i].getID())) {
                                    mlTreeNearEnemy = treesNearEnemy[i].location;
                                    treeNearEnemy = treesNearEnemy[i].getID();
                                }
                            }
                        }
                    } else if (nTargetRobot != 0) {
                        if (mlTargetRobotLoc != null) {
                            TreeInfo treesNearEnemy[] = rc.senseNearbyTrees(mlTargetRobotLoc, RobotType.SCOUT.sensorRadius, enemy);
                            for (int i = 0; i < treesNearEnemy.length; i++) {
                                if (rc.canSenseTree(treesNearEnemy[i].getID())) {
                                    mlTreeNearEnemy = treesNearEnemy[i].location;
                                    treeNearEnemy = treesNearEnemy[i].getID();
                                }
                            }
                        }
                    }

                } else {
                    bBattleMode = false;
                }

                if (nearbyTrees.length > 0) {
                    for (int i = 0; i < nearbyTrees.length; i++) {
                        if (bBattleMode == false) {
                            if (nearbyTrees[i].containedBullets != 0) {
                                if (nTargetTree == 0) {
                                    nTargetTree = nearbyTrees[i].getID();
                                    mlTargetTree = nearbyTrees[i].location;
                                    //System.out.println("Got a target");
                                }
                            }
                        }
                        if (nearbyTrees[i].getTeam() == enemy) {
                            int nXPos = 500;
                            while (true) {
                                if (rc.readBroadcast(nXPos) == 0) {
                                    rc.broadcast(nXPos, Math.round(nearbyTrees[i].location.x));
                                    rc.broadcast(nXPos + 1, Math.round(nearbyTrees[i].location.y));
                                    break;
                                } else {
                                    nXPos++;
                                    if (nXPos > 600) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }

                if (bBattleMode == true) {
                    if (treeNearEnemy != 0) {
                        currentLoc = rc.getLocation();
                        Direction dirToTreeNearEnemy = new Direction(currentLoc, mlTreeNearEnemy);
                        if (!rc.hasMoved()) {
                            if (rc.canMove(mlTreeNearEnemy)) {
                                rc.move(mlTreeNearEnemy);
                            } else {
                                tryMove(dirToTreeNearEnemy);
                            }
                        }
                    }

                    if (rc.canFireSingleShot()) {
                        MapLocation currentLoc = rc.getLocation();
                        if (nTargetGardener != 0) {
                            rc.fireSingleShot(new Direction(currentLoc, mlTargetGardenLoc));
                        } else if (nTargetRobot != 0) {
                            rc.fireSingleShot(new Direction(currentLoc, mlTargetRobotLoc));
                        }
                    }
                }

                if (bBattleMode == false) {
                    if (nTargetTree != 0) {
                        currentLoc = rc.getLocation();
                        Direction dirTargetTree = new Direction(currentLoc, mlTargetTree);
                        if (!rc.hasMoved()) {
                            if (rc.canMove(mlTargetTree)) {
                                rc.move(mlTargetTree);
                            } else {
                                tryMove(dirTargetTree);
                            }
                            if (rc.canShake(nTargetTree)) {
                                rc.shake(nTargetTree);
                                nTargetTree = 0;
                            }
                        }
                    }
                }


                if (nTargetTree == 0 && bBattleMode == false) {
                    while (true) {
                        if (rc.readBroadcast(XPos) != 0) {
                            MapLocation enemyWhereAbouts = new MapLocation(rc.readBroadcast(XPos), rc.readBroadcast((XPos + 1)));
                            currentLoc = rc.getLocation();
                            if (!rc.hasMoved()) {
                                tryMove(new Direction(currentLoc, enemyWhereAbouts));
                            }
                            break;
                        } else {
                            XPos +=2;
                            if (XPos > 498) {
                                break;
                            }
                        }
                    }
                    int nCount = 0;
                    while(!rc.hasMoved() && nCount != 4)
                    if (!rc.hasMoved() && rc.canMove(dirList[currentDirection])) {
                        tryMove(dirList[currentDirection]);
                    } else {
                        currentDirection = moveOnDiagonal();
                    }
                }
                //System.out.println(bBattleMode);
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Scout Exception");
                e.printStackTrace();
            }
        }

    }


    static void runTank() throws GameActionException {
        while (true) {
            try {
                currentLoc = rc.getLocation();
                RobotInfo[] robots = rc.senseNearbyRobots(-1, enemy);

                for (RobotInfo b : robots) {
                    if (b.getTeam() != homeTeam && rc.canFireSingleShot()) {
                        Direction enemyRoboDir = rc.getLocation().directionTo(b.getLocation());
                        System.out.println("SHOOTING THE ENEMY");
                        rc.fireSingleShot(enemyRoboDir);
                        break;
                    }
                }
                TreeInfo[] trees = rc.senseNearbyTrees();
                for (TreeInfo t : trees) {
                    if (t.team != homeTeam) {
                        if (rc.canFireSingleShot()) {
                            rc.fireSingleShot(currentLoc.directionTo(t.location));
                            System.out.println("SHOOTING THE TREE");
                        } else if (!rc.hasMoved()) {
                            tryMove(currentLoc.directionTo(t.location));
                            break;
                        }
                    }
                }
                if (!rc.hasMoved()) {
                    // this is for trees
                    System.out.println("GONNA READ BROADCAST FOR TREES");
                    int XPos = 500;
                    while (true) {
                        if (rc.readBroadcast(XPos) > 0) {
                            MapLocation enemyTree = new MapLocation(rc.readBroadcast(XPos), rc.readBroadcast(XPos + 1));
                            if (rc.canSenseLocation(enemyTree)) {
                                if (rc.senseTreeAtLocation(enemyTree) != null) {
                                    if (rc.canFireSingleShot()) {
                                        rc.fireSingleShot(currentLoc.directionTo(enemyTree));
                                        System.out.println("SHOOTING THE TREE from Broadcast");
                                    }
                                    Direction dirToEnemyTree = new Direction(currentLoc, enemyTree);
                                    tryMove(dirToEnemyTree);
                                    break;
                                } else {
                                    rc.broadcast(XPos, 0);
                                    rc.broadcast(XPos + 1, 0);
                                }
                            } else {
                                System.out.println("GO TO THE ENEMY TREE");
                                Direction dirToEnemyTree = new Direction(currentLoc, enemyTree);
                                tryMove(dirToEnemyTree);
                                break;
                            }

                        } else {
                            XPos += 2;
                            if (XPos > 600) {
                                System.out.println("Nothing to read");
                                if(!tryMove(dirList[currentDirection])) {
                                    currentDirection = moveOnDiagonal();
                                }
                                break;
                            }
                        }
                    }
                }
                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println("Tank Exception");
                e.printStackTrace();
            }
        }
    }




    /**
     * Returns a random Direction
     *
     * @return a random Direction
     */

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
        if (rc.canMove(dir) && rc.onTheMap(currentLoc.add(dir, 3))) {
            rc.move(dir);
            currentLoc = rc.getLocation();
            return true;
        }

        // Now try a bunch of similar angles
        //boolean moved = false;
        int currentCheck = 1;

        while (currentCheck <= checksPerSide) {
            // Try the offset of the left side
            if (rc.canMove(dir.rotateLeftDegrees(degreeOffset * currentCheck)) && rc.onTheMap(currentLoc.add(dir, 3))) {
                rc.move(dir.rotateLeftDegrees(degreeOffset * currentCheck));
                currentLoc = rc.getLocation();
                return true;
            }
            // Try the offset on the right side
            if (rc.canMove(dir.rotateRightDegrees(degreeOffset * currentCheck)) && rc.onTheMap(currentLoc.add(dir, 3))) {
                rc.move(dir.rotateRightDegrees(degreeOffset * currentCheck));
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

        // Get relevant bullet information
        Direction propagationDirection = bullet.dir;
        MapLocation bulletLocation = bullet.location;

        // Calculate bullet relations to this robot
        Direction directionToRobot = bulletLocation.directionTo(currentLoc);
        float distToRobot = bulletLocation.distanceTo(currentLoc);
        float theta = propagationDirection.radiansBetween(directionToRobot);

        // If theta > 90 degrees, then the bullet is traveling away from us and we can break early
        if (Math.abs(theta) > Math.PI / 2) {
            return false;
        }

        // distToRobot is our hypotenuse, theta is our angle, and we want to know this length of the opposite leg.
        // This is the distance of a line that goes from currentLoc and intersects perpendicularly with propagationDirection.
        // This corresponds to the smallest radius circle centered at our location that would intersect with the
        // line that is the path of the bullet.
        float perpendicularDist = (float) Math.abs(distToRobot * Math.sin(theta)); // soh cah toa :)

        return (perpendicularDist <= rc.getType().bodyRadius);
    }
    static boolean trySidestep(BulletInfo bullet) throws GameActionException{

        Direction towards = bullet.getDir();
        MapLocation leftGoal = rc.getLocation().add(towards.rotateLeftDegrees(90), rc.getType().bodyRadius);
        MapLocation rightGoal = rc.getLocation().add(towards.rotateRightDegrees(90), rc.getType().bodyRadius);

        return tryMove(towards.rotateLeftDegrees(90)) || tryMove(towards.rotateRightDegrees(90));
    }

    static void dodge() throws GameActionException {
        boolean bHasMoved = false;
        BulletInfo[] bullets = rc.senseNearbyBullets();
        for (BulletInfo bi : bullets) {
            if (willCollideWithMe(bi)) {
                if (!bHasMoved) {
                    bHasMoved = trySidestep(bi);
                }
            }
        }

    }

}