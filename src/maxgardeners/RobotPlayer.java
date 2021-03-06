package maxgardeners;

import battlecode.common.*;

import java.util.*;

/**
 * Created by Max_Inspiron15 on 1/10/2017.
 */
public strictfp class RobotPlayer {
    static RobotController rc;
    static Random myRand;
    @SuppressWarnings("unused")
    // Keep broadcast channels
    static int ARCHON_CHANNEL = 5;
    static int GARDENER_CHANNEL = 6;
    static int LUMBERJACK_CHANNEL = 7;


    // Keep important numbers here
    static int ARCHON_MAX = 3;
    static int GARDENER_MAX = 5;
    static int LUMBERJACK_MAX = 10;

    public static void run(RobotController rc) throws GameActionException {
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        myRand = new Random(rc.getID());
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
        }
    }


    static void runArchon() throws GameActionException {
        // use boolean to see if already set to zero - smth like initialarchonloc
        while (true) {
            try {
                if (!rc.hasMoved()) wander();
                Direction dir = randomDirection();
                int prevNumGard = rc.readBroadcast(GARDENER_CHANNEL);
              //  int prevNumArchon = rc.readBroadcast(ARCHON_CHANNEL);
                if (rc.getID() == 2 || rc.getID() == 3 /*|| prevNumArchon > ARCHON_MAX*/) {
                    rc.broadcast(GARDENER_CHANNEL, 0);
                  //  rc.broadcast(ARCHON_CHANNEL, 1);
                    System.out.println("I'm the prime archon");
                } else {
                    /*if (prevNumArchon >= ARCHON_MAX) {
                        rc.broadcast(ARCHON_CHANNEL, 0);
                    } else {
                        rc.broadcast(ARCHON_CHANNEL, prevNumArchon + 1);
                    }*/
                }
                System.out.println("ARCHON CHANNEL:" + rc.readBroadcast(ARCHON_CHANNEL));
                if (prevNumGard < GARDENER_MAX && rc.canHireGardener(dir)) {
                    rc.hireGardener(dir);
                    rc.broadcast(GARDENER_CHANNEL, prevNumGard + 1);
                }
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runGardener() throws GameActionException {
        while (true) {
            try {
                int prev = rc.readBroadcast(GARDENER_CHANNEL);
                rc.broadcast(GARDENER_CHANNEL, prev + 1);
                wander();
                Direction dir = randomDirection();
                int prevNumGard = rc.readBroadcast(LUMBERJACK_CHANNEL);
                if (prevNumGard <= LUMBERJACK_MAX && rc.canBuildRobot(RobotType.LUMBERJACK, dir)) {
                    rc.buildRobot(RobotType.LUMBERJACK, dir);
                    rc.broadcast(LUMBERJACK_CHANNEL, prevNumGard + 1);
                }

                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runSoldier() throws GameActionException {
        while (true) {
            try {
                wander();
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    static void runLumberjack() throws GameActionException {
        while (true) {
            try {
                RobotInfo[] bots = rc.senseNearbyRobots();
                for (RobotInfo b : bots) {
                    if (b.getTeam() != rc.getTeam() && rc.canStrike()) {
                        rc.strike();
                        Direction chase = rc.getLocation().directionTo(b.getLocation());
                        if (rc.canMove(chase)) {
                            rc.move(chase);
                        }
                        break;
                    }
                }
                TreeInfo[] trees = rc.senseNearbyTrees();
                for (TreeInfo t : trees) {
                    if (rc.canChop(t.getLocation())) {
                        rc.chop(t.getLocation());
                        break;
                    }
                }
                if (!rc.hasAttacked()) {
                    wander();
                }
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
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


    public static Direction randomDirection() {
        return (new Direction(myRand.nextFloat() * 2 * (float) Math.PI));
    }
}