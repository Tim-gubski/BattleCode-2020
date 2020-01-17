package Insomnia;

import battlecode.common.*;

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
    static int hqX1;
    static int hqX2;
    static int hqX3;
    static int hqY1;
    static int hqY2;
    static int hqY3;
    static int currentTarget = 0;
    static MapLocation hqLoc;
    static MapLocation enemyHQ;
    static MapLocation refLoc;
    static MapLocation schLoc;
    static MapLocation lastSoup;
    static MapLocation mapMid;
    static boolean isSchool = false;
    static boolean isCenter = false;
    static boolean isRefinery = false;
    static boolean enemyHQKnown = false;
    static boolean layerFilled = false;
    static boolean isChosenOne = false;

    /**
     * run() is the method that is called when a robot is instantiated in the
     * Battlecode world. If this method returns, the robot dies!
     *
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
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLoc = robot.location;
                }
            }
        }

        if (rc.getType() == RobotType.DESIGN_SCHOOL) {
            trySendChain("069");
            //tryChainSchool(rc.getLocation().x, rc.getLocation().y);
        }
        if (rc.getType() == RobotType.FULFILLMENT_CENTER) {
            trySendChain("666");
            //tryChainCenter(rc.getLocation().x, rc.getLocation().y);
        }
        if (rc.getType() == RobotType.REFINERY) {
            trySendChain("273");
            //tryChainRefinery(rc.getLocation().x, rc.getLocation().y);
        }

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
        RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED);
        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.DELIVERY_DRONE && rc.canShootUnit(robot.getID()) && robot.team != rc.getTeam()) {
                rc.shootUnit(robot.getID());
            }
            if (!isChosenOne && robot.getType() == RobotType.DELIVERY_DRONE && rc.getTeam() == robot.team) {
                if (iChooseYou(robot.getID())) {
                    isChosenOne = true;
                }

            }
        }

    }

    static void runMiner() throws GameActionException {
        chainScan();
        //Check if refinery has been created
        if (!isRefinery) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.REFINERY && robot.team == rc.getTeam()) {
                    isRefinery = true;
                    refLoc = robot.location;
                }
            }
            //If Refinery doesn't exist and robot is in a set radius around the HQ, create a Refinery
            if (radiusTo(hqLoc) >= 36 && radiusTo(hqLoc) <= 60 && !isRefinery) {
                tryBuild(RobotType.REFINERY, dirTo(hqLoc));
            }
        }

//        VAPES!!!
        if (radiusTo(hqLoc) >= 45 && radiusTo(hqLoc) <= 98) {
            tryBuild(RobotType.VAPORATOR, dirTo(hqLoc));
        }

        //Check if design school has been created
        if (!isSchool) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.DESIGN_SCHOOL && robot.team == rc.getTeam()) {
                    isSchool = true;
                    schLoc = robot.location;
                }
            }
            //If school doesn't exist and robot is in a set radius around the HQ, create a design school
            if (radiusTo(hqLoc) >= 25 && radiusTo(hqLoc) <= 28 && !isSchool && isRefinery) {
                tryBuild(RobotType.DESIGN_SCHOOL, dirTo(hqLoc));
            }
        }
        //Check if fulfillment center has been created
        if (!isCenter) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.FULFILLMENT_CENTER && robot.team == rc.getTeam()) {
                    isCenter = true;
                }
            }
            //If center doesn't exist and robot is in a set radius around the HQ, create a fulfillment center
            if (radiusTo(hqLoc) >= 25 && radiusTo(hqLoc) <= 28 && !isCenter && isRefinery) {
                tryBuild(RobotType.FULFILLMENT_CENTER, dirTo(hqLoc));
            }
        }

        //Try refining in all directions
        for (Direction dir : directions) {
            if (tryRefine(dir)) {
                System.out.println("I refined soup! " + rc.getTeamSoup());
            }
        }
        //Try mining in all directions
        for (Direction dir : directions) {
            if (tryMine(dir)) {
                System.out.println("I mined soup! " + rc.getSoupCarrying());
                lastSoup = rc.getLocation().add(dir);
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
        } else if (lastSoup != null) {
            moveTowards(lastSoup);
            if (rc.getLocation() == lastSoup) {
                lastSoup = null;
            }
        }
        tryMove(randomDirection());

    }

    static void runRefinery() throws GameActionException {

        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
//        Build a landscaper in the closest possible direction to the HQ
        int landscaperLimit = 8; // This is a temporary landscaper limit.

        if (landscaperCount < landscaperLimit) {
            if (tryBuild(RobotType.LANDSCAPER, dirTo(hqLoc))) {
                landscaperCount++;
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        int droneLimit = 10; // This is a temporary drone limit.
        if (droneCount < droneLimit) {
            if (tryBuild(RobotType.DELIVERY_DRONE, dirTo(hqLoc))) {
                droneCount++;
            }
        }
    }

    static void runLandscaper() throws GameActionException {
        if (!layerFilled) {
            boolean empty = false;
            for (Direction dir : directions) {
                if (rc.senseRobotAtLocation(hqLoc.add(dir)) == null) {
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

        if (layerFilled) {
            tryDigDirt(dirTo(hqLoc).opposite());
            tryDropDirt(rc.getLocation());
        }
    }

    static void runDeliveryDrone() throws GameActionException {
        chainScan();
        Team enemy = rc.getTeam().opponent();
        MapLocation[] targets = new MapLocation[]{new MapLocation(rc.getMapWidth() - hqLoc.x, rc.getMapHeight() - hqLoc.y),
            new MapLocation(rc.getMapWidth() - hqLoc.x, hqLoc.y),
            new MapLocation(hqLoc.x, rc.getMapHeight() - hqLoc.y)};
        if (isChosenOne) {
            droneMoveTowards(targets[currentTarget]);
            if (rc.getLocation().equals(targets[currentTarget])) {
                currentTarget += 1;
            }
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED);
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.HQ && robot.team == rc.getTeam().opponent()) {
                    tryChainEnemy(robot.location.x, robot.location.y);
                    isChosenOne = false;
                }
            }
        } else {
            // Code here for peasant drones
            if (!rc.isCurrentlyHoldingUnit()) {
                // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
                RobotInfo[] enemies = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
                if (enemies.length > 0) {
                    // Pick up a first robot within range
                    for (RobotInfo robot : enemies) {
                        if (robot.getType() == RobotType.LANDSCAPER) {
                            if (rc.canPickUpUnit(robot.getID())) {
                                rc.pickUpUnit(robot.getID());
                                break;
                            } else {
                                droneMoveTowards(robot.location);
                            }
                        }
                    }
                } else if (enemyHQ != null) {
                    droneMoveTowards(enemyHQ);
                } else {
                    droneMoveTowards(mapMid);
                }
            }
        }
    }

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
        return (int) (Math.pow(Math.abs(rc.getLocation().x - loc.x), 2) + Math.pow(Math.abs(rc.getLocation().y - loc.y), 2));
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
        if (rc.isReady() && rc.canMove(dir)) {
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
        } else {
            return false;
        }
    }

    static boolean droneMoveTowards(MapLocation loc) throws GameActionException {
        if (loc == null) {
            return false;
        }
        if (droneMoveTowards(dirTo(loc))) {
            droneMoveTowards(dirTo(loc));
            return true;
        } else {
            return false;
        }
    }

    static boolean droneMoveTowards(Direction dir) throws GameActionException {
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
     - 069 <--> tryChainSchool   (Determines whether or not there is a Design School built)
     - 69  <--> iChooseYou       (Determines the Chosen Drone)
     - 666 <--> tryChainCenter   (Determines whether or not there is a Fulfillment Center built)
     - 273 <--> tryChainRefinery (Determines whether or not there is a Refinery built)
     - 420 <--> tryChainEnemy    (Determines whether or not the enemy HQ Location is known)
     */
    static void trySendChain(String chainType, String id, int x, int y) throws GameActionException {
        int[] trans = {
            0,
            0,
            0,
            0,
            0,
            0,
            0
        };
        
    }
    
    static void trySendChain(String chainType) throws GameActionException {
        String message = null;
        String falseMsg = 05 + Integer.toString((int)(Math.random() * 10000));
        int[] trans = {
            0,
            0,
            0,
            0,
            0,
            0,
            0
        };
        if (chainType.equals("069")) {
            message = chainType + String.format("%04d", (int)(Math.random() * 1000));
        }
        if (chainType.equals("666")) {
            message = chainType + String.format("%04d", (int)(Math.random() * 1000));
        }
        if (chainType.equals("273")) {
            message = chainType + String.format("%04d", (int)(Math.random() * 1000));
        }
        if (message == null) {
            return;
        }
        trans[0] = Integer.parseInt(message);
        trans[1] = Integer.parseInt(falseMsg);
        falseMsg = 05 + String.format("%05d", (int)(Math.random() * 100000));
        trans[2] = Integer.parseInt(falseMsg);
        falseMsg = 13 + String.format("%05d", (int)(Math.random() * 100000));
        trans[3] = Integer.parseInt(falseMsg);
        falseMsg = 11 + String.format("%05d", (int)(Math.random() * 100000));
        trans[4] = Integer.parseInt(falseMsg);
        falseMsg = 00 + String.format("%05d", (int)(Math.random() * 100000));
        trans[5] = Integer.parseInt(falseMsg);
        falseMsg = 55 + String.format("%05d", (int)(Math.random() * 100000));
        trans[6] = Integer.parseInt(falseMsg);
        
        if (rc.canSubmitTransaction(trans, 5)) {
            rc.submitTransaction(trans, 5);
        }
    }

    static void chainScan() throws GameActionException {
        Transaction[] trans = rc.getBlock(rc.getRoundNum() - 1);
        int loop = 0;
        for (Transaction tran : trans) {
            int[] me = tran.getMessage();
            if (Integer.toString(me[loop]).substring(0, 3).equals("069")) {
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
            }
            if (Integer.toString(me[loop]).substring(0, 2).equals("69")) {
                String id = Integer.toString(me[loop]).substring(2, 7);
                if (Integer.parseInt(id) == rc.getID()) {
                    isChosenOne = true;
                }
            }
            loop++;
        }
    }

    static void tryChainSchool(int x, int y) throws GameActionException {
        String message = "069" + String.format("%02d", x) + String.format("%02d", y);
        // The string you want to be an integer array.
        String[] integerStrings = message.split("");
        // Splits each spaced integer into a String array.
        int[] integers = new int[integerStrings.length];
        // Creates the integer array.
        for (int i = 0; i < integers.length; i++) {
            integers[i] = Integer.parseInt(integerStrings[i]);
            //Parses the integer for each string.
        }
        if (rc.canSubmitTransaction(integers, 2)) {
            rc.submitTransaction(integers, 2);
        }
    }

    static boolean iChooseYou(int id) throws GameActionException {
        String message = "69" + Integer.toString(id);
        // The string you want to be an integer array.
        String[] integerStrings = message.split("");
        // Splits each spaced integer into a String array.
        int[] integers = new int[integerStrings.length];
        // Creates the integer array.
        for (int i = 0; i < integers.length; i++) {
            integers[i] = Integer.parseInt(integerStrings[i]);
            //Parses the integer for each string.
        }
        if (rc.canSubmitTransaction(integers, 2)) {
            rc.submitTransaction(integers, 2);
            return true;
        }
        return false;
    }

    static void tryChainCenter(int x, int y) throws GameActionException {
        String message = "666" + String.format("%02d", x) + String.format("%02d", y);
        // The string you want to be an integer array.
        String[] integerStrings = message.split("");
        // Splits each spaced integer into a String array.
        int[] integers = new int[integerStrings.length];
        // Creates the integer array.
        for (int i = 0; i < integers.length; i++) {
            integers[i] = Integer.parseInt(integerStrings[i]);
            //Parses the integer for each string.
        }
        if (rc.canSubmitTransaction(integers, 2)) {
            rc.submitTransaction(integers, 2);
        }
    }

    static boolean tryChainEnemy(int x, int y) throws GameActionException {
        String message = "420" + String.format("%02d", x) + String.format("%02d", y);
        // The string you want to be an integer array.
        String[] integerStrings = message.split("");
        // Splits each spaced integer into a String array.
        int[] integers = new int[integerStrings.length];
        // Creates the integer array.
        for (int i = 0; i < integers.length; i++) {
            integers[i] = Integer.parseInt(integerStrings[i]);
            //Parses the integer for each string.
        }
        if (rc.canSubmitTransaction(integers, 2)) {
            rc.submitTransaction(integers, 2);
            return true;
        }
        return false;
    }

    static void tryChainRefinery(int x, int y) throws GameActionException {
        String message = "273" + String.format("%02d", x) + String.format("%02d", y);
        // The string you want to be an integer array.
        String[] integerStrings = message.split("");
        // Splits each spaced integer into a String array.
        int[] integers = new int[integerStrings.length];
        // Creates the integer array.
        for (int i = 0; i < integers.length; i++) {
            integers[i] = Integer.parseInt(integerStrings[i]);
            //Parses the integer for each string.
        }
        if (rc.canSubmitTransaction(integers, 2)) {
            rc.submitTransaction(integers, 2);
        }
    }
}
