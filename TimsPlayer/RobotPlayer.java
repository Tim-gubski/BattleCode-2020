package TimsPlayer;

import battlecode.common.*;

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
    static MapLocation hqLoc;
    static MapLocation refLoc;
    static MapLocation schLoc;
    static MapLocation desLoc;
    static MapLocation lastSoup;
    static boolean isSchool = false;
    static boolean isCenter = false;
    static boolean isRefinery = false;
    static boolean enemyHQKnown = false;
    static boolean landscaperDeployed = false;
    static boolean layerFilled = false;
    static boolean atPeace = false;
    static boolean empty = true;

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
        turnCount = 0;

        if (hqLoc == null) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.HQ && robot.team == rc.getTeam()) {
                    hqLoc = robot.location;
                }
            }
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
        RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.NET_GUN_SHOOT_RADIUS_SQUARED, rc.getTeam().opponent());
        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.DELIVERY_DRONE && rc.canShootUnit(robot.getID())) {
                rc.shootUnit(robot.getID());
            }
        }

    }

    static void runMiner() throws GameActionException {
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
            if (radiusTo(hqLoc) >= 36 && radiusTo(hqLoc) <= 60 && !isRefinery && isSchool && isCenter) {
                tryBuild(RobotType.REFINERY, dirTo(hqLoc));
            }
        }

//        VAPES!!!
        if (radiusTo(hqLoc) >= 45 && radiusTo(hqLoc) <= 98) {
            tryBuild(RobotType.VAPORATOR, dirTo(hqLoc));
        }

        //Check if fulfillment center has been created
        if (!isCenter) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.FULFILLMENT_CENTER && robot.team == rc.getTeam()) {
                    isCenter = true;
                    desLoc = robot.location;
                }
            }
            //If center doesn't exist and robot is in a set radius around the HQ, create a fulfillment center
            if (radiusTo(hqLoc) >= 4 && radiusTo(hqLoc) <= 8 && !isCenter && rc.getRoundNum() > 300) {
                tryBuild(RobotType.FULFILLMENT_CENTER, hqLoc.add(Direction.NORTH));
                tryBuild(RobotType.FULFILLMENT_CENTER, hqLoc.add(Direction.EAST));
                tryBuild(RobotType.FULFILLMENT_CENTER, hqLoc.add(Direction.SOUTH));
                tryBuild(RobotType.FULFILLMENT_CENTER, hqLoc.add(Direction.WEST));
                
            }
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
            if (radiusTo(hqLoc) >= 4 && radiusTo(hqLoc) <= 8 && !isSchool && rc.getRoundNum() > 300) {
                tryBuild(RobotType.DESIGN_SCHOOL, hqLoc.add(hqLoc.directionTo(desLoc).opposite()));
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
        } else if(lastSoup!=null) {
            moveTowards(lastSoup);
            if(rc.getLocation()==lastSoup){
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
        int landscaperLimit = 16; // This is a temporary landscaper limit.
        if (rc.getRoundNum() > 420) {
            landscaperLimit += 6;
        }
        if (landscaperCount < landscaperLimit) {
            if (tryBuild(RobotType.LANDSCAPER, dirTo(hqLoc))) {
                landscaperCount++;
            }
        }
    }

    static void runFulfillmentCenter() throws GameActionException {
//        int droneLimit = 50; // This is a temporary landscaper limit.
//        if (droneCount < droneLimit) {
//            if (tryBuild(RobotType.DELIVERY_DRONE, dirTo(hqLoc))) {
//                tryBuild(RobotType.DELIVERY_DRONE, dirTo(hqLoc));
//                droneCount++;
//            }
//        }
    }

    static void runLandscaper() throws GameActionException {
        //find Design School
        if (!isSchool) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.DESIGN_SCHOOL && robot.team == rc.getTeam()) {
                    isSchool = true;
                    schLoc = robot.location;
                }
            }
        }

        if (radiusTo(hqLoc) < 3 || radiusTo(hqLoc) > 8 && !layerFilled) {
            moveTowards(dirTo(hqLoc));
        }

        if (rc.getLocation().isAdjacentTo(schLoc) && rc.getRoundNum() < 430) {
            moveTowards(dirTo(schLoc).opposite());
        }

        if(rc.getLocation().isAdjacentTo(hqLoc) && rc.getRoundNum() > 450){
            tryDigDirt(Direction.CENTER);
            MapLocation bestTile = null;
            int lowest = 10000;
            for (Direction dir : directions) {
                if (rc.senseElevation(rc.getLocation().add(dir)) < lowest && rc.getLocation().add(dir).distanceSquaredTo(hqLoc) > 2) {
                    lowest = rc.senseElevation(rc.getLocation().add(dir));
                    bestTile = rc.getLocation().add(dir);
                }
            }
            tryDropDirt(bestTile);
        }
        if(!rc.getLocation().isAdjacentTo(hqLoc) && rc.getRoundNum() > 450){
            tryDigDirt(dirTo(hqLoc).opposite());
            tryDropDirt(rc.getLocation());
        }

    }

    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        MapLocation enemyHQ = null;
        int hqX;
        int hqY;
        int loops = 0;
        RobotInfo[] enemies = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);
        if (!enemyHQKnown) {
            if (enemies.length > 0) {
                for (RobotInfo robot : enemies) {
                    if (robot.getType() == RobotType.HQ) {
                        enemyHQ = robot.getLocation();
                        hqX = enemyHQ.x;
                        hqY = enemyHQ.y;
                        tryChainHQ(hqX, hqY);
                    }
                }
            }
        }
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            if (enemies.length > 0) {
                // Pick up a first robot within range
                for (RobotInfo robot : enemies) {
                    if (robot.getType() == RobotType.LANDSCAPER || robot.getType() == RobotType.MINER) {
                        if (rc.canPickUpUnit(robot.getID())) {
                            rc.pickUpUnit(robot.getID());
                            System.out.println("I picked up " + enemies[loops].getID() + "!");
                        break;
                        } else {
                            moveTowards(dirTo(robot.getLocation()));
                        }
                    }
                    loops++;
                }
            }
        } else if (enemyHQKnown) {
            tryMove(dirTo(enemyHQ));

        } else {
            tryMove(randomDirection());
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

    static boolean moveTowards(MapLocation loc) throws GameActionException {
        if(loc==null){
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


    static void tryChainSoup(int x, int y) throws GameActionException {
//        if (rc.canSubmitTransaction(message, 2)) {
//            rc.submitTransaction(message, 2);
//        }
    }


    static void tryChainHQ(int x, int y) throws GameActionException {
        int[] message = new int[7];
        String msgString = null;
        for (int i = 0; i < 7; i++) {
            if (x < 10 && y < 10) {
                msgString = "4200" + x + "0" + y;
            } else if (x < 10 && y > 10) {
                msgString = "4200" + x + y;
            } else if (x > 10 && y < 10) {
                msgString = "420" + x + "0" + y;
            } else {
                msgString = "420" + x + y;
            }
            message[i] = Integer.parseInt(msgString);
        }
        if (rc.canSubmitTransaction(message, rc.getTeamSoup()) && rc.getTeamSoup() > 150) {
            rc.submitTransaction(message, rc.getTeamSoup());
            enemyHQKnown = true;
        }
    }
}
