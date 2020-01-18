package Insomnia;

import battlecode.common.*;

import java.awt.*;
import java.util.Arrays;

public strictfp class RobotPlayer {

    static RobotController rc;

    static Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST
    };
    static RobotType[] spawnedByMiner = {RobotType.REFINERY, RobotType.VAPORATOR, RobotType.DESIGN_SCHOOL,
        RobotType.FULFILLMENT_CENTER, RobotType.NET_GUN};

    //Variable Declaration/Initialization
    static int turnCount;
    static int minerCount;
    static int landscaperCount;
    static int droneCount;
    static int mapHeight;
    static int mapWidth;
    static int seppukuCount = 0;
    static int hqX1;
    static int hqX2;
    static int hqX3;
    static int hqY1;
    static int hqY2;
    static int hqY3;
    static int currentTarget = 0;
    static int notChosenCurrentTarget = 0;
    static int vapeCount = 0;
    static MapLocation hqLoc;
    static MapLocation enemyHQ;
    static MapLocation refLoc;
    static MapLocation schLoc;
    static MapLocation lastSoup;
    static MapLocation mapMid;
    static MapLocation[] hqBorder = new MapLocation[8];
    static MapLocation[] robotBorder = new MapLocation[8];
    static MapLocation aboveUnit;
    static MapLocation belowUnit;
    static boolean isSchool = false;
    static boolean isCenter = false;
    static boolean isRefinery = false;
    static boolean enemyHQKnown = false;
    static boolean layerFilled = false;
    static boolean isChosenOne = false;
    static boolean isChosenMiner = false;
    static boolean takeStep = false;

    /**
     * run() is the method that is called when a robot is instantiated in the
     * Battlecode world. If this method returns, the robot dies!
     */
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;
        mapHeight = rc.getMapHeight();
        mapWidth = rc.getMapWidth();
        mapMid = new MapLocation((int) (mapWidth / 2), (int) (mapHeight / 2));

        turnCount = 0;

        if (hqLoc == null) {
            if (rc.getType() == RobotType.HQ) {
                hqLoc = rc.getLocation();
            }
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLoc = robot.location;
                }
            }
        }
        if (hqLoc == null) {
            hqChainScan();
        }

        if (rc.getType() == RobotType.DESIGN_SCHOOL) {
            trySendChain("774", rc.getLocation().x, rc.getLocation().y);
        }
        if (rc.getType() == RobotType.FULFILLMENT_CENTER) {
            trySendChain("666", rc.getLocation().x, rc.getLocation().y);
        }
        if (rc.getType() == RobotType.REFINERY) {
            trySendChain("273", rc.getLocation().x, rc.getLocation().y);
        }
        if (rc.getType() == RobotType.HQ) {
            trySendChain("911", rc.getLocation().x, rc.getLocation().y);
        }
        if (rc.getType() == RobotType.VAPORATOR) {
            trySendChain("877", rc.getLocation().x, rc.getLocation().y);
        }

        if (hqLoc != null) {
            hqBorder[0] = new MapLocation(hqLoc.x, hqLoc.y + 1);
            hqBorder[1] = new MapLocation(hqLoc.x + 1, hqLoc.y + 1);
            hqBorder[2] = new MapLocation(hqLoc.x + 1, hqLoc.y);
            hqBorder[3] = new MapLocation(hqLoc.x + 1, hqLoc.y - 1);
            hqBorder[4] = new MapLocation(hqLoc.x, hqLoc.y - 1);
            hqBorder[5] = new MapLocation(hqLoc.x - 1, hqLoc.y - 1);
            hqBorder[6] = new MapLocation(hqLoc.x - 1, hqLoc.y);
            hqBorder[7] = new MapLocation(hqLoc.x - 1, hqLoc.y + 1);
        }

        robotBorder[0] = new MapLocation(rc.getLocation().x, rc.getLocation().y + 1);
        robotBorder[1] = new MapLocation(rc.getLocation().x + 1, rc.getLocation().y + 1);
        robotBorder[2] = new MapLocation(rc.getLocation().x + 1, rc.getLocation().y);
        robotBorder[3] = new MapLocation(rc.getLocation().x + 1, rc.getLocation().y - 1);
        robotBorder[4] = new MapLocation(rc.getLocation().x, rc.getLocation().y - 1);
        robotBorder[5] = new MapLocation(rc.getLocation().x - 1, rc.getLocation().y - 1);
        robotBorder[6] = new MapLocation(rc.getLocation().x - 1, rc.getLocation().y);
        robotBorder[7] = new MapLocation(rc.getLocation().x - 1, rc.getLocation().y + 1);

        while (true) {
            turnCount += 1;
            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:
                        runHQ();
                        break;
                    case MINER:
                        runMiner();
                        break;
                    case REFINERY:
                        runRefinery();
                        break;
                    case VAPORATOR:
                        runVaporator();
                        break;
                    case DESIGN_SCHOOL:
                        runDesignSchool();
                        break;
                    case FULFILLMENT_CENTER:
                        runFulfillmentCenter();
                        break;
                    case LANDSCAPER:
                        runLandscaper();
                        break;
                    case DELIVERY_DRONE:
                        runDeliveryDrone();
                        break;
                    case NET_GUN:
                        runNetGun();
                        break;
                }

                // Clock.yield() makes the robot wait until the next turn, then it will perform this loop again
                Clock.yield();

            } catch (Exception e) {
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();
            }
        }
    }

    static void runHQ() throws GameActionException {
        MapLocation nearestSoup;
        if (minerCount < 5) {
            if (tryBuild(RobotType.MINER, Direction.NORTHEAST)) {
                minerCount++;
            }
        }
        if (rc.getRoundNum() > 20 && rc.getTeamSoup() > 400 && minerCount < 6) {
            if (tryBuild(RobotType.MINER, Direction.NORTHEAST)) {
                minerCount++;
            }
        }
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.DELIVERY_DRONE && rc.canShootUnit(robot.getID()) && robot.team != rc.getTeam()) {
                rc.shootUnit(robot.getID());
            }
            if (!isChosenOne && robot.getType() == RobotType.DELIVERY_DRONE && rc.getTeam() == robot.team) {
                if (trySendChain("69", robot.getID())) {
                    isChosenOne = true;
                }

            }
            if (rc.getRoundNum() > 20 && rc.getTeamSoup() > 400 && !isChosenMiner && robot.type == RobotType.MINER && rc.getTeam() == robot.team) {
                if (trySendChain("96", robot.getID())) {
                    isChosenMiner = true;
                }
            }
        }

    }

    static void runMiner() throws GameActionException {
        chainScan();

        if (isChosenMiner) {
            //Check if design school has been created
            if (!takeStep) {
                moveTowards(dirTo(hqLoc).opposite());
                takeStep = true;
            }

            if (vapeCount >= 3) {
                if (!isSchool) {
                    if (tryBuild(RobotType.DESIGN_SCHOOL, dirTo(hqLoc).opposite())) {
                        isSchool = true;
                    }
                }
                //Check if fulfillment center has been created
                if (!isCenter) {
                    if (tryBuild(RobotType.FULFILLMENT_CENTER, dirTo(hqLoc).opposite())) {
                        isCenter = true;
                    }
                }
            }
            if (isCenter && isSchool) {
                isChosenMiner = false;
            }
            //^^CHOSEN ONE CODE^^//
        } else {
            //Try mining in all directions
            if (!isRefinery) {
                RobotInfo[] robots = rc.senseNearbyRobots();
                for (RobotInfo robot : robots) {
                    if (robot.type == RobotType.REFINERY && robot.team == rc.getTeam()) {
                        isRefinery = true;
                        refLoc = robot.location;
                    }
                }
                //If Refinery doesn't exist and robot is in a set radius around the HQ, create a Refinery
                if (radiusTo(hqLoc) >= 8 && !isRefinery) {
                    tryBuild(RobotType.REFINERY, dirTo(hqLoc).opposite());
                }
            }
            if (vapeCount < 3 && rc.getLocation().distanceSquaredTo(hqLoc) > 8) {
                tryBuild(RobotType.VAPORATOR, dirTo(hqLoc).opposite());
            }
            if (lastSoup == null) {
                MapLocation[] soupLocs = rc.senseNearbySoup();
                if (soupLocs.length > 0) {
                    if (tryMine(dirTo(soupLocs[0]))) {
                        lastSoup = rc.getLocation().add(dirTo(soupLocs[0]));
                    } else {
                        moveTowards(dirTo(soupLocs[0]));
                    }
                }
            } else if (lastSoup != null) {
                moveTowards(lastSoup);
            }

            for (Direction dir : directions) {
                if (tryMine(dir)) {
                    lastSoup = rc.getLocation().add(dir);
                }
            }

            //Try refining in all directions
            for (Direction dir : directions) {
                if (tryRefine(dir)) {
                }
            }

            //If soup capacity is full, return to HQ to deposit soup
            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
                if (isRefinery) {
                    moveTowards(refLoc);
                } else {
                    moveTowards(hqLoc);
                }
            }
            if (moveTowards(findSoup())) {
            }
//            else if (lastSoup != null) {
//                moveTowards(lastSoup);
//                if (rc.getLocation() == lastSoup) {
//                    lastSoup = null;
//                }
//            }
            tryMove(randomDirection());
        }
    }

    static void runRefinery() throws GameActionException {

        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {
    }

    static void runDesignSchool() throws GameActionException {
//        Build a landscaper in the closest possible direction to the HQ
        int landscaperLimit = 9; // This is a temporary landscaper limit.

        if (landscaperCount < landscaperLimit) {
            if (tryBuild(RobotType.LANDSCAPER, dirTo(hqLoc))) {
                landscaperCount++;
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        int droneLimit = 50; // This is a temporary drone limit.
        if (droneCount < droneLimit && rc.getTeamSoup() > 200) {
            if (tryBuild(RobotType.DELIVERY_DRONE, dirTo(hqLoc).opposite())) {
                droneCount++;
            } else if (tryBuild(RobotType.DELIVERY_DRONE, dirTo(hqLoc))) {
                droneCount++;
            }
        }
    }

    static void runLandscaper() throws GameActionException {
        if (hqLoc == null) {
            System.out.println("Im peeing");
        }
        if (!layerFilled && rc.canSenseLocation(hqLoc.add(dirTo(hqLoc)))) {
            boolean empty = false;
            for (Direction dir : directions) {
                if ((trySenseRobotAtLocation(hqLoc.add(dir)) == null || trySenseRobotAtLocation(hqLoc.add(dir)).type != RobotType.LANDSCAPER) && rc.canSenseLocation(hqLoc.add(dir))) {
                    empty = true;
                }
            }
            if (!empty) {
                layerFilled = true;
            }

        }

        if (!rc.getLocation().isAdjacentTo(hqLoc)) {
            moveTowards(hqLoc);
        }

        if (rc.getLocation().isAdjacentTo(hqLoc) && (layerFilled || rc.getRoundNum() > 350)) {
            if (tryDigDirt(dirTo(hqLoc).opposite())) {
                tryDigDirt(dirTo(hqLoc).opposite());
            }
            tryDropDirt(rc.getLocation());
        }
    }

    static void runDeliveryDrone() throws GameActionException {
        chainScan();
        Team enemy = rc.getTeam().opponent();
        MapLocation[] targets = new MapLocation[]{new MapLocation(rc.getMapWidth() - hqLoc.x, rc.getMapHeight() - hqLoc.y),
            new MapLocation(rc.getMapWidth() - hqLoc.x, hqLoc.y),
            new MapLocation(hqLoc.x, rc.getMapHeight() - hqLoc.y)};
        if (isChosenOne && rc.getRoundNum() > 750) {
            droneMoveTowards(targets[currentTarget]);
            if (rc.getLocation().equals(targets[currentTarget])) {
                currentTarget += 1;
            }
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.HQ && robot.team == enemy) {
                    if (trySendChain("420", robot.location.x, robot.location.y)) {
                        isChosenOne = false;
                    }

                }
            }
        } else if (isChosenOne) {
            droneMoveTowards(mapMid);
        }
        System.out.println(!isChosenOne && rc.getRoundNum() > 750 && enemyHQKnown);
        // wait till round 750 and until you know where enemy hq is
        if (!isChosenOne && rc.getRoundNum() > 750 && enemyHQKnown) {
            // run if you havent captured a landscaper yet
            if (!rc.isCurrentlyHoldingUnit()) {
                //scan for robots
                RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), enemy);
                //if any robots were found and round is above 1000
                if (robots.length > 0 && rc.getRoundNum() > 1000) {
                    boolean noLandscapers = true;
                    int closestID = 0;
                    int closestDistance = 1000;
                    //run through all robots
                    for (RobotInfo robot : robots) {
                        //only run if the robot found is a landscaper
                        if (robot.getType() == RobotType.LANDSCAPER && robot.team == enemy) {
                            noLandscapers = false;
                            //if you can pick it up please do so
                            if (rc.canPickUpUnit(robot.getID())) {
                                rc.pickUpUnit(robot.getID());
                                break;
                            }
                            //find the closest landscaper
                            if (rc.getLocation().distanceSquaredTo(robot.location) < closestDistance) {
                                closestDistance = rc.getLocation().distanceSquaredTo(robot.location);
                                closestID = robot.ID;
                            }
                        }
                    }
                    // go to the closest landscaper
                    if (closestID != 0) {
                        droneMoveTowards(rc.senseRobot(closestID).location);
                    }
                    //if you couldn't find any landscapers go to the enemyHQ
                    if (noLandscapers) {
                        droneSwarmAround(enemyHQ);
                    }
                    // if no robots found swarm around enemy HQ
                } else {
                    droneSwarmAround(enemyHQ);
                }
                // if robot picked up the run the hell away
            } else {
                aboveUnit = new MapLocation(rc.getLocation().x, rc.getLocation().y + 1);
                belowUnit = new MapLocation(rc.getLocation().x, rc.getLocation().y - 1);
                if (rc.senseFlooding(aboveUnit) && rc.canDropUnit(dirTo(aboveUnit))) {
                    rc.dropUnit(Direction.NORTH);
                } else if (rc.senseFlooding(belowUnit) && rc.canDropUnit(dirTo(belowUnit))) {
                    rc.dropUnit(Direction.SOUTH);
                } else {
                    droneMoveTowards(dirTo(enemyHQ).opposite());
                }
            }
            // Huddle in the middle till the time comes
        } else if (!isChosenOne) {
            RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), enemy);
            System.out.println(robots.length);
            if (robots.length > 0) {
                for (RobotInfo robot : robots) {
                    if (robot.getType() == RobotType.MINER || robot.getType() == RobotType.LANDSCAPER) {
                        if (rc.canPickUpUnit(robot.getID())) {
                            rc.pickUpUnit(robot.getID());
                            break;
                        } else {
                            droneMoveTowards(rc.senseRobot(robot.getID()).location);
                        }
                    }
                }
            } else {
                if (notChosenCurrentTarget == 0) {
                    droneSwarmAround(mapMid);
                    if (rc.getLocation().isWithinDistanceSquared(mapMid, RobotType.NET_GUN.sensorRadiusSquared + 5)) {
                        notChosenCurrentTarget = 1;
                    }
                }
                if (notChosenCurrentTarget == 1) {
                    droneSwarmAround(hqLoc);
                }

            }
        }

    }
// Code here for peasant drones
//            if (rc.getRoundNum() < 800) {

//                }
//            if (!loopBroke) {
//                droneMoveTowards(mapMid);
//            }
//        }
//    } else if (rc.getRoundNum() >= 800) {
//        droneMoveTowards(enemyHQ);
//    }
//}
    static void runNetGun() throws GameActionException {

    }

    // HELPFUL METHODS!!!
    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static Direction findSoup() throws GameActionException {
        // Scan tiles for soup
        MapLocation currentLocation = rc.getLocation();
        for (int xOffset = -5; xOffset <= 5; xOffset++) {
            for (int yOffset = -5; yOffset <= 5; yOffset++) {
                if (trySenseSoup(new MapLocation(currentLocation.x + xOffset, currentLocation.y + yOffset)) > 0) {
                    return dirTo(new MapLocation(currentLocation.x + xOffset, currentLocation.y + yOffset));
                }
            }
        }
        return randomDirection();
    }

    static Direction dirTo(MapLocation loc) throws GameActionException {
        return rc.getLocation().directionTo(loc);
    }

    static int radiusTo(MapLocation loc) throws GameActionException {
        return rc.getLocation().distanceSquaredTo(hqLoc);
    }

    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    // ACTION METHODS
    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions) {
            if (tryMove(dir)) {
                return true;
            }
        }
        return false;
        // MapLocation loc = rc.getLocation();
        // if (loc.x < 10 && loc.x < loc.y)
        //     return tryMove(Direction.EAST);
        // else if (loc.x < 10)
        //     return tryMove(Direction.SOUTH);
        // else if (loc.x > loc.y)
        //     return tryMove(Direction.WEST);
        // else
        //     return tryMove(Direction.NORTH);
    }

    static RobotInfo trySenseRobotAtLocation(MapLocation loc) throws GameActionException {
        if (rc.canSenseLocation(loc)) {
            return rc.senseRobotAtLocation(loc);
        } else {
            return null;
        }
    }

    static int trySenseSoup(MapLocation loc) throws GameActionException {
        if (rc.isReady() && rc.canSenseLocation(loc)) {
            return rc.senseSoup(loc);
        } else {
            return 0;
        }
    }

    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir) && !rc.senseFlooding(rc.getLocation().add(dir))) {
            rc.move(dir);
            return true;
        } else {
            return false;
        }
    }

    static boolean tryDroneMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir) && rc.onTheMap(rc.getLocation().add(dir))) {
            rc.move(dir);
            return true;
        } else {
            return false;
        }
    }

    static boolean moveTowards(MapLocation loc) throws GameActionException {
        if (loc == null) {
            return false;
        }
        if (moveTowards(dirTo(loc))) {
            return true;
        } else {
            return false;
        }
    }

    static boolean moveTowards(Direction dir) throws GameActionException {
        if (rc.getRoundNum() % 30 > 15) {
            if (tryMove(dir)) {
                return true;
            } else if (tryMove(dir.rotateRight())) {
                return true;
            } else if (tryMove(dir.rotateRight().rotateRight())) {
                return true;
            } else if (tryMove(dir.rotateLeft())) {
                return true;
            } else if (tryMove(dir.rotateLeft().rotateLeft())) {
                return true;
            } else if (tryMove(dir.rotateRight().rotateRight().rotateRight())) {
                return true;
            } else if (tryMove(dir.rotateLeft().rotateLeft().rotateLeft())) {
                return true;
            } else if (tryMove(dir.opposite())) {
                return true;
            } else {
                return false;
            }
        } else {
            if (tryMove(dir)) {
                return true;
            } else if (tryMove(dir.rotateLeft())) {
                return true;
            } else if (tryMove(dir.rotateLeft().rotateLeft())) {
                return true;
            } else if (tryMove(dir.rotateRight())) {
                return true;
            } else if (tryMove(dir.rotateRight().rotateRight())) {
                return true;
            } else if (tryMove(dir.rotateRight().rotateRight().rotateRight())) {
                return true;
            } else if (tryMove(dir.rotateLeft().rotateLeft().rotateLeft())) {
                return true;
            } else if (tryMove(dir.opposite())) {
                return true;
            } else {
                return false;
            }
        }
    }

    static boolean droneSwarmAround(MapLocation loc) throws GameActionException {
        if (!rc.getLocation().add(dirTo(loc)).isWithinDistanceSquared(loc, RobotType.NET_GUN.sensorRadiusSquared)) {
            if (tryDroneMove(dirTo(loc))) {
                return true;
            }
        }
        if (!rc.getLocation().add(dirTo(loc).rotateRight()).isWithinDistanceSquared(loc, RobotType.NET_GUN.sensorRadiusSquared)) {
            if (tryDroneMove(dirTo(loc).rotateRight())) {
                return true;
            }
        }
        if (!rc.getLocation().add(dirTo(loc).rotateRight().rotateRight()).isWithinDistanceSquared(loc, RobotType.NET_GUN.sensorRadiusSquared)) {
            if (tryDroneMove(dirTo(loc).rotateRight().rotateRight())) {
                return true;
            }
        }
        if (!rc.getLocation().add(dirTo(loc).rotateLeft()).isWithinDistanceSquared(loc, RobotType.NET_GUN.sensorRadiusSquared)) {
            if (tryDroneMove(dirTo(loc).rotateLeft())) {
                return true;
            }
        }
        if (!rc.getLocation().add(dirTo(loc).rotateLeft().rotateLeft()).isWithinDistanceSquared(loc, RobotType.NET_GUN.sensorRadiusSquared)) {
            if (tryDroneMove(dirTo(loc).rotateLeft().rotateLeft())) {
                return true;
            }
        }
        if (!rc.getLocation().add(dirTo(loc).rotateRight().rotateRight().rotateRight()).isWithinDistanceSquared(loc, RobotType.NET_GUN.sensorRadiusSquared)) {
            if (tryDroneMove(dirTo(loc).rotateRight().rotateRight().rotateRight())) {
                return true;
            }
        }
        if (!rc.getLocation().add(dirTo(loc).rotateLeft().rotateLeft().rotateLeft()).isWithinDistanceSquared(loc, RobotType.NET_GUN.sensorRadiusSquared)) {
            if (tryDroneMove(dirTo(loc).rotateLeft().rotateLeft().rotateLeft())) {
                return true;
            }
        }
        return false;
    }

    static boolean droneMoveTowards(MapLocation loc) throws GameActionException {
        if (loc == null) {
            return false;
        }
        if (droneMoveTowards(dirTo(loc))) {
            return true;
        } else {
            return false;
        }
    }

    static boolean droneMoveTowards(Direction dir) throws GameActionException {
        if (rc.getRoundNum() % 30 > 15) {
            if (tryDroneMove(dir)) {
                return true;
            } else if (tryDroneMove(dir.rotateRight())) {
                return true;
            } else if (tryDroneMove(dir.rotateRight().rotateRight())) {
                return true;
            } else if (tryDroneMove(dir.rotateLeft())) {
                return true;
            } else if (tryDroneMove(dir.rotateLeft().rotateLeft())) {
                return true;
            } else {
                return false;
            }
        } else {
            if (tryDroneMove(dir)) {
                return true;
            } else if (tryDroneMove(dir.rotateLeft())) {
                return true;
            } else if (tryDroneMove(dir.rotateLeft().rotateLeft())) {
                return true;
            } else if (tryDroneMove(dir.rotateRight())) {
                return true;
            } else if (tryDroneMove(dir.rotateRight().rotateRight())) {
                return true;
            } else {
                return false;
            }
        }
    }

    static boolean tryBuild(RobotType type, MapLocation loc) throws GameActionException {
        if (rc.canBuildRobot(type, dirTo(loc)) && rc.getLocation().isAdjacentTo(loc)) {
            rc.buildRobot(type, dirTo(loc));
            return true;
        } else {
            return false;
        }
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else if (rc.canBuildRobot(type, dir.rotateRight())) {
            rc.buildRobot(type, dir.rotateRight());
            return true;
        } else if (rc.canBuildRobot(type, dir.rotateLeft())) {
            rc.buildRobot(type, dir.rotateLeft());
            return true;
        } else if (rc.canBuildRobot(type, dir.rotateRight().rotateRight())) {
            rc.buildRobot(type, dir.rotateRight().rotateRight());
            return true;
        } else if (rc.canBuildRobot(type, dir.rotateLeft().rotateLeft())) {
            rc.buildRobot(type, dir.rotateLeft().rotateLeft());
            return true;
        } else if (rc.canBuildRobot(type, dir.rotateRight().rotateRight().rotateRight())) {
            rc.buildRobot(type, dir.rotateRight().rotateRight().rotateRight());
            return true;
        } else if (rc.canBuildRobot(type, dir.rotateLeft().rotateLeft().rotateLeft())) {
            rc.buildRobot(type, dir.rotateLeft().rotateLeft().rotateLeft());
            return true;
        } else {
            return false;
        }
    }

    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else {
            return false;
        }
    }

    static boolean tryDropDirt(MapLocation loc) throws GameActionException {
        if (rc.isReady() && rc.canDepositDirt(dirTo(loc)) && (rc.getLocation().isAdjacentTo(loc) || rc.getLocation() == loc)) {
            rc.depositDirt(dirTo(loc));
            return true;
        } else {
            return false;
        }
    }

    static boolean tryDigDirt(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDigDirt(dir)) {
            rc.digDirt(dir);
            return true;
        } else if (rc.isReady() && rc.canDigDirt(dir.rotateRight())) {
            rc.digDirt(dir.rotateRight());
            return true;
        } else if (rc.isReady() && rc.canDigDirt(dir.rotateLeft())) {
            rc.digDirt(dir.rotateLeft());
            return true;
        } else if (rc.isReady() && rc.canDigDirt(dir.rotateRight().rotateRight())) {
            rc.digDirt(dir.rotateRight());
            return true;
        } else if (rc.isReady() && rc.canDigDirt(dir.rotateLeft().rotateLeft())) {
            rc.digDirt(dir.rotateRight());
            return true;
        } else {
            return false;
        }

    }

    static int trySenseElevation(MapLocation loc) throws GameActionException {
        if (rc.isReady() && rc.canSenseLocation(loc)) {
            return rc.senseElevation(loc);
        } else {
            return 10000;
        }
    }

    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else {
            return false;
        }
    }

    /*
    **IMPORTANT**
    List of blockchain codes!!!
     - 774 <--> tryChainSchool   (Determines whether or not there is a Design School built)
     - 69  <--> iChooseYou       (Determines the Chosen Drone)
     - 666 <--> tryChainCenter   (Determines whether or not there is a Fulfillment Center built)
     - 273 <--> tryChainRefinery (Determines whether or not there is a Refinery built)
     - 420 <--> tryChainEnemy    (Determines whether or not the enemy HQ Location is known)
     */
    static boolean trySendChain(String chainType, int id) throws GameActionException {
        String message = null;
        String falseMsg = 05 + Integer.toString((int) (Math.random() * 10000));
        int[] trans = {
            0,
            0,
            0,
            0,
            0,
            0,
            0
        };
        if (chainType.equals("69") || chainType.equals("96")) {
            message = chainType + id;
        }

        if (message == null) {
            return false;
        }
        trans[0] = Integer.parseInt(message);
        trans[1] = Integer.parseInt(falseMsg);
        falseMsg = 12 + String.format("%05d", (int) (Math.random() * 10000));
        trans[2] = Integer.parseInt(falseMsg);
        falseMsg = 13 + String.format("%05d", (int) (Math.random() * 10000));
        trans[3] = Integer.parseInt(falseMsg);
        falseMsg = 18 + String.format("%05d", (int) (Math.random() * 10000));
        trans[4] = Integer.parseInt(falseMsg);
        falseMsg = 95 + String.format("%05d", (int) (Math.random() * 10000));
        trans[5] = Integer.parseInt(falseMsg);
        falseMsg = 54 + String.format("%05d", (int) (Math.random() * 10000));
        trans[6] = Integer.parseInt(falseMsg);
        System.out.println("Trying to send transaction...");
        rc.submitTransaction(trans, 6);

        return true;
    }

    static boolean trySendChain(String chainType, int x, int y) throws GameActionException {
        String message = null;
        String falseMsg = 05 + Integer.toString((int) (Math.random() * 10000));
        int[] trans = {
            0,
            0,
            0,
            0,
            0,
            0,
            0
        };
        if (chainType.equals("774")) {
            message = chainType + String.format("%02d", x) + String.format("%02d", y);
        }
        if (chainType.equals("666")) {
            message = chainType + String.format("%02d", x) + String.format("%02d", y);
        }
        if (chainType.equals("273")) {
            message = chainType + String.format("%02d", x) + String.format("%02d", y);
        }
        //Enemy HQ Found
        if (chainType.equals("420")) {
            message = chainType + String.format("%02d", x) + String.format("%02d", y);
        }
        //Our HQ
        if (chainType.equals("911")) {
            message = chainType + String.format("%02d", x) + String.format("%02d", y);
        }
        //Im a vape
        if (chainType.equals("877")) {
            message = chainType + String.format("%02d", x) + String.format("%02d", y);
        }
        //Found soup
        if (chainType.equals("939")) {
            message = chainType + String.format("%02d", x) + String.format("%02d", y);
        }
        if (message == null) {
            return false;
        }
        trans[0] = Integer.parseInt(message);
        trans[1] = Integer.parseInt(falseMsg);
        falseMsg = 12 + String.format("%05d", (int) (Math.random() * 100000));
        trans[2] = Integer.parseInt(falseMsg);
        falseMsg = 13 + String.format("%05d", (int) (Math.random() * 100000));
        trans[3] = Integer.parseInt(falseMsg);
        falseMsg = 18 + String.format("%05d", (int) (Math.random() * 100000));
        trans[4] = Integer.parseInt(falseMsg);
        falseMsg = 95 + String.format("%05d", (int) (Math.random() * 100000));
        trans[5] = Integer.parseInt(falseMsg);
        falseMsg = 54 + String.format("%05d", (int) (Math.random() * 100000));
        trans[6] = Integer.parseInt(falseMsg);

        if (rc.canSubmitTransaction(trans, 5)) {
            rc.submitTransaction(trans, 5);
        }
        return true;
    }

    static void chainScan() throws GameActionException {
        Transaction[] trans = rc.getBlock(rc.getRoundNum() - 1);
        int loop = 0;
        for (Transaction tran : trans) {
            int[] me = tran.getMessage();
            if (Integer.toString(me[loop]).substring(0, 3).equals("774")) {
                isSchool = true;
            }
            if (Integer.toString(me[loop]).substring(0, 3).equals("666")) {
                isCenter = true;
            }
            if (Integer.toString(me[loop]).substring(0, 3).equals("273")) {
                String x = Integer.toString(me[loop]).substring(3, 5);
                String y = Integer.toString(me[loop]).substring(5, 7);
                isRefinery = true;
                refLoc = new MapLocation(Integer.parseInt(x), Integer.parseInt(y));
            }
            if (Integer.toString(me[loop]).substring(0, 3).equals("420")) {
                String x = Integer.toString(me[loop]).substring(3, 5);
                String y = Integer.toString(me[loop]).substring(5, 7);
                enemyHQ = new MapLocation(Integer.parseInt(x), Integer.parseInt(y));
                enemyHQKnown = true;
            }
            if (Integer.toString(me[loop]).substring(0, 3).equals("877")) {
                String x = Integer.toString(me[loop]).substring(3, 5);
                String y = Integer.toString(me[loop]).substring(5, 7);
                vapeCount += 1;
            }
            if (Integer.toString(me[loop]).substring(0, 2).equals("69")) {
                String id = Integer.toString(me[loop]).substring(2, 7);
                if (Integer.parseInt(id) == rc.getID()) {
                    isChosenOne = true;
                }
            }
            if (Integer.toString(me[loop]).substring(0, 2).equals("96")) {
                String id = Integer.toString(me[loop]).substring(2, 7);
                if (Integer.parseInt(id) == rc.getID()) {
                    isChosenMiner = true;
                }
            }
            loop++;
        }
    }

    static void hqChainScan() throws GameActionException {
        Transaction[] trans = rc.getBlock(1);
        int loop = 0;
        for (Transaction tran : trans) {
            int[] me = tran.getMessage();
            if (Integer.toString(me[loop]).substring(0, 3).equals("911")) {
                String x = Integer.toString(me[loop]).substring(3, 5);
                String y = Integer.toString(me[loop]).substring(5, 7);
                hqLoc = new MapLocation(Integer.parseInt(x), Integer.parseInt(y));
            }
            loop++;
        }
    }
}
