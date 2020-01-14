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

    static int turnCount;
    static int minerCount;
    static MapLocation HQloc;
    static boolean isSchool = false;
    /**
     * run() is the method that is called when a robot is instantiated in the Battlecode world.
     * If this method returns, the robot dies!
     **/
    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {
        // This is the RobotController object. You use it to perform actions from this robot,
        // and to get information on its current status.
        RobotPlayer.rc = rc;

        turnCount = 0;

        if(HQloc==null){
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if(robot.type == RobotType.HQ && robot.team == rc.getTeam()){
                    HQloc=robot.location;
                }
            }
        }
        //System.out.println("I'm a " + rc.getType() + " and I just got created!");
        while (true) {
            turnCount += 1;

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode
            try {
                // Here, we've separated the controls into a different method for each RobotType.
                // You can add the missing ones or rewrite this into your own control structure.
                //System.out.println("I'm a " + rc.getType() + "! Location " + rc.getLocation());
                switch (rc.getType()) {
                    case HQ:                 runHQ();                break;
                    case MINER:              runMiner();             break;
                    case REFINERY:           runRefinery();          break;
                    case VAPORATOR:          runVaporator();         break;
                    case DESIGN_SCHOOL:      runDesignSchool();      break;
                    case FULFILLMENT_CENTER: runFulfillmentCenter(); break;
                    case LANDSCAPER:         runLandscaper();        break;
                    case DELIVERY_DRONE:     runDeliveryDrone();     break;
                    case NET_GUN:            runNetGun();            break;
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
        if (minerCount<5) {
            for (Direction dir : directions)
                if (tryBuild(RobotType.MINER, dir)) {
                    minerCount++;
                }
        }
    }

    static void runMiner() throws GameActionException {
        //Check if design school has been created
        if (!isSchool) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.type == RobotType.DESIGN_SCHOOL && robot.team == rc.getTeam()) {
                    isSchool = true;
                }
            }
        }
        //If school doesn't exist and robot is in a set radius around the HQ, create a design school
        if(radiusTo(HQloc)>=25 && radiusTo(HQloc)<=34 && !isSchool){
            tryBuild(RobotType.DESIGN_SCHOOL,dirTo(HQloc));
        }
        //Try refining in all directions
        for (Direction dir : directions) {
            if (tryRefine(dir))
                System.out.println("I refined soup! " + rc.getTeamSoup());
        }
        //Try mining in all directions
        for (Direction dir : directions) {
            if (tryMine(dir))
                System.out.println("I mined soup! " + rc.getSoupCarrying());
        }
        //If soup capacity is full, return to HQ to deposit soup
        if(rc.getSoupCarrying()==RobotType.MINER.soupLimit){
            moveTowards(HQloc);
        }
        //If nothing else works, move in a random direction
        tryMove(randomDirection());
    }

    static void runRefinery() throws GameActionException {
        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {

    }

    static void runDesignSchool() throws GameActionException {
        //Build a landscaper in the closest possible direction to the HQ
        tryBuild(RobotType.LANDSCAPER,dirTo(HQloc));
    }

    static void runFulfillmentCenter() throws GameActionException {

    }

    static void runLandscaper() throws GameActionException {
        tryMove(randomDirection())
    }

    static void runDeliveryDrone() throws GameActionException {
        Team enemy = rc.getTeam().opponent();
        if (!rc.isCurrentlyHoldingUnit()) {
            // See if there are any enemy robots within striking range (distance 1 from lumberjack's radius)
            RobotInfo[] robots = rc.senseNearbyRobots(GameConstants.DELIVERY_DRONE_PICKUP_RADIUS_SQUARED, enemy);

            if (robots.length > 0) {
                // Pick up a first robot within range
                rc.pickUpUnit(robots[0].getID());
                System.out.println("I picked up " + robots[0].getID() + "!");
            }
        } else {
            // No close robots, so search for robots within sight radius
            tryMove(randomDirection());
        }
    }

    static void runNetGun() throws GameActionException {

    }



    // HELPFUL METHODS!!!

    static Direction randomDirection() {
        return directions[(int) (Math.random() * directions.length)];
    }

    static Direction dirTo(MapLocation loc) throws GameActionException {
        return rc.getLocation().directionTo(loc);
    }

    static int radiusTo(MapLocation loc) throws GameActionException{
        return (int) (Math.pow(Math.abs(rc.getLocation().x-loc.x),2) + Math.pow(Math.abs(rc.getLocation().y-loc.y),2));
    }

    static RobotType randomSpawnedByMiner() {
        return spawnedByMiner[(int) (Math.random() * spawnedByMiner.length)];
    }

    // ACTION METHODS

    static boolean tryMove() throws GameActionException {
        for (Direction dir : directions)
            if (tryMove(dir))
                return true;
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

    static boolean tryMove(Direction dir) throws GameActionException {
        // System.out.println("I am trying to move " + dir + "; " + rc.isReady() + " " + rc.getCooldownTurns() + " " + rc.canMove(dir));
        if (rc.isReady() && rc.canMove(dir)) {
            rc.move(dir);
            return true;
        } else return false;
    }

    static boolean moveTowards(MapLocation loc) throws GameActionException {
        if(tryMove(dirTo(loc))) {
            return true;
        }else if(tryMove(dirTo(loc).rotateRight())){
            return true;
        }else if(tryMove(dirTo(loc).rotateLeft())) {
            return true;
        }else{
            return false;
        }
    }

    static boolean tryBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        } else if(rc.isReady() && rc.canBuildRobot(type, dir.rotateRight())){
            rc.buildRobot(type, dir.rotateRight());
            return true;
        }else if(rc.isReady() && rc.canBuildRobot(type, dir.rotateLeft())){
            rc.buildRobot(type, dir.rotateLeft());
            return true;
        }else return false;
    }

    static boolean tryMine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canMineSoup(dir)) {
            rc.mineSoup(dir);
            return true;
        } else return false;
    }

    static boolean tryRefine(Direction dir) throws GameActionException {
        if (rc.isReady() && rc.canDepositSoup(dir)) {
            rc.depositSoup(dir, rc.getSoupCarrying());
            return true;
        } else return false;
    }


    static void tryBlockchain() throws GameActionException {
        if (turnCount < 3) {
            int[] message = new int[7];
            for (int i = 0; i < 7; i++) {
                message[i] = 123;
            }
            if (rc.canSubmitTransaction(message, 10))
                rc.submitTransaction(message, 10);
        }
        // System.out.println(rc.getRoundMessages(turnCount-1));
    }
}
