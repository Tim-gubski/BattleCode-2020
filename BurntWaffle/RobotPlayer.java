package BurntWaffle;

import battlecode.common.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

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
    static int schoolCount = 0;
    static int centerCount = 0;
    static int closest = 10000;
    static int steps = 0;
    static int turtleRound = 120;
    static int waffleHeight = 8;
    static int scapersDisplaced = 0;
    static int minersDisplaced = 0;
    static float accumulation = 0;
    static MapLocation hqLoc;
    static MapLocation enemyHQ;
    static ArrayList<MapLocation> refLoc = new ArrayList<>();
    static MapLocation schLoc;
    static MapLocation lastSoup;
    static MapLocation mapMid;
    static MapLocation aboveUnit;
    static MapLocation belowUnit;
    static MapLocation lastTarget;
    static MapLocation rushSchLoc;
    static MapLocation firstNetGun;
    static MapLocation[] explore;
    static MapLocation[] targets;
    static int exploreIndex = 0;
    static boolean exploring = true;
    static boolean isSchool = false;
    static boolean isCenter = false;
    static boolean enemyHQKnown = false;
    static boolean layerFilled = false;
    static boolean isChosenOne = false;
    static boolean isChosenMiner = false;
    static boolean rushSchool = false;
    static boolean messageSent = false;
    static boolean takeStep = false;
    static boolean turtleMiner = false;
    static boolean rushScaper = false;
    static boolean rushFactory = false;
    static boolean endGame = false;
    static boolean protectScaper = false;
    static boolean isAlreadyHoldingUnit = false;
    static Direction lastDirection;

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
        if(hqLoc == null){
            hqLoc = new MapLocation(10,10);
        }

        if (rc.getType() == RobotType.DESIGN_SCHOOL) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.HQ && robot.team != rc.getTeam()) {
                    enemyHQ = robot.location;
                    rushFactory = true;
                }
            }
            if(!rushFactory) {
                trySendChain("774", rc.getLocation().x, rc.getLocation().y);
            }
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
        if (rc.getType() == RobotType.LANDSCAPER) {
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.HQ && robot.team != rc.getTeam()) {
                    enemyHQ=robot.location;
                    rushScaper=true;
                    break;
                }
            }

            if(!rushScaper && (int) (Math.random()*3) == 2){
                protectScaper = true;
            }
        }

        explore = new MapLocation[]{mapMid, new MapLocation(mapWidth/2,hqLoc.y),
                new MapLocation(hqLoc.x,mapHeight/2),
                new MapLocation(hqLoc.x,mapHeight),
                new MapLocation(hqLoc.x,0),
                new MapLocation(mapWidth,hqLoc.y),
                new MapLocation(0,hqLoc.y)};
        targets = new MapLocation[]{new MapLocation(mapWidth - hqLoc.x, mapHeight - hqLoc.y),
                new MapLocation(mapWidth - hqLoc.x, hqLoc.y),
                new MapLocation(hqLoc.x, mapHeight - hqLoc.y)};


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
        chainScan();
        //relay numbers of items
        if(endGame && rc.getRoundNum()%4==0){
            masterChainSend();
        }

        //build miners
        if (minerCount < 5) {
            if(rc.senseNearbySoup().length>0) {
                if (tryBuild(RobotType.MINER, dirTo(rc.senseNearbySoup()[0]))) {
                    minerCount++;
                }
            }else{
                if (tryBuild(RobotType.MINER, randomDirection())) {
                    minerCount++;
                }
            }
        }
        if(rc.getRoundNum()>turtleRound && minerCount<6 && rc.getTeamSoup()>80){
            if(rc.senseNearbySoup().length>0) {
                if (tryBuild(RobotType.MINER, dirTo(rc.senseNearbySoup()[0]))) {
                    minerCount++;
                }
            }else{
                if (tryBuild(RobotType.MINER, randomDirection())) {
                    minerCount++;
                }
            }
        }
        System.out.println(minerCount);
        if(endGame && minerCount<10 && rc.getRoundNum()%15==0){
            for(Direction dir : directions) {
                if (tryBuild(RobotType.MINER, dir)) {
                    minerCount++;
                    break;
                }
            }
        }
        //HQ Fanciness
        RobotInfo[] robots = rc.senseNearbyRobots();
        int landscaperCount = 0;
        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.DELIVERY_DRONE && rc.canShootUnit(robot.getID()) && robot.team != rc.getTeam()) {
                rc.shootUnit(robot.getID());
            }
            if (!isChosenOne && robot.getType() == RobotType.DELIVERY_DRONE && rc.getTeam() == robot.team) {
                if (trySendChain("69", robot.getID())) {
                    isChosenOne = true;
                }

            }
//            if(!isChosenMiner && robot.type == RobotType.MINER && rc.getTeam() == robot.team){
//                if (trySendChain("96", robot.getID())) {
//                    isChosenMiner = true;
//                }
//            }
            if(!turtleMiner && robot.type == RobotType.MINER && rc.getTeam() == robot.team && robot.location.isAdjacentTo(rc.getLocation()) && rc.getRoundNum()>turtleRound){
                if (trySendChain("11", robot.ID)) {
                    System.out.println("chosen");
                    turtleMiner=true;
                }
            }
            if(robot.type == RobotType.LANDSCAPER && rc.getTeam() == robot.team){
                landscaperCount++;
            }
        }
        if(!endGame && landscaperCount>4){
            if(trySendChain("116",rc.getLocation().x,rc.getLocation().y)){
                endGame = true;
            }
        }

    }

    static void runMiner() throws GameActionException {
        System.out.println(endGame);
        chainScan();
        scanRefinery();
        maybeDie();
        //Try mining in all directions
        if(isChosenMiner){
            if (rc.getLocation().isWithinDistanceSquared(targets[currentTarget],rc.getCurrentSensorRadiusSquared()-10) && currentTarget<2) {
                currentTarget += 1;
            }
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.HQ && robot.team != rc.getTeam()) {
                    enemyHQ=robot.location;
                    enemyHQKnown=true;
                    if(!messageSent && trySendChain("420", robot.location.x, robot.location.y)){
                        messageSent = true;
                    }
                }
                if (robot.getType() == RobotType.DESIGN_SCHOOL && robot.team == rc.getTeam()) {
                    rushSchool=true;
                    rushSchLoc=robot.location;
                }
            }

            if((!rushSchool) && enemyHQKnown){
                if(rc.getLocation().isAdjacentTo(enemyHQ)) {
                    if(tryBuild(RobotType.DESIGN_SCHOOL, enemyHQ.add(Direction.NORTH))){
                        rushSchool=true;
                        rushSchLoc=enemyHQ.add(Direction.NORTH);
                    }
                    if(tryBuild(RobotType.DESIGN_SCHOOL, enemyHQ.add(Direction.WEST))){
                        rushSchool=true;
                        rushSchLoc=enemyHQ.add(Direction.WEST);
                    }
                    if(tryBuild(RobotType.DESIGN_SCHOOL, enemyHQ.add(Direction.SOUTH))){
                        rushSchool=true;
                        rushSchLoc=enemyHQ.add(Direction.SOUTH);
                    }
                    if(tryBuild(RobotType.DESIGN_SCHOOL, enemyHQ.add(Direction.EAST))){
                        rushSchool=true;
                        rushSchLoc=enemyHQ.add(Direction.EAST);
                    }
                }
            }

            if(enemyHQ==null) {
                bugNav(targets[currentTarget]);
            }else if(!rushSchool&&!rc.getLocation().isAdjacentTo(enemyHQ)){
                bugNav(enemyHQ);
            }else if(!rushSchool&&rc.getLocation().isAdjacentTo(enemyHQ)){
                swarmTo(enemyHQ);
            }else if(rc.getLocation()!=enemyHQ.add(enemyHQ.directionTo(rushSchLoc).opposite())){
                tryMove(dirTo(enemyHQ.add(enemyHQ.directionTo(rushSchLoc).opposite())));
            }

        //Turtle code
        } else if(turtleMiner){
            System.out.println("i am turtle");
            if(!isSchool){
                if(tryBuild(RobotType.DESIGN_SCHOOL,hqLoc.add(Direction.NORTHEAST))){
                    isSchool=true;
                    schLoc=hqLoc.add(Direction.NORTHEAST);
                }
                if(tryBuild(RobotType.DESIGN_SCHOOL,hqLoc.add(Direction.NORTHWEST))){
                    isSchool=true;
                    schLoc=hqLoc.add(Direction.NORTHWEST);
                }
                if(tryBuild(RobotType.DESIGN_SCHOOL,hqLoc.add(Direction.SOUTHEAST))){
                    isSchool=true;
                    schLoc=hqLoc.add(Direction.SOUTHEAST);
                }
                if(tryBuild(RobotType.DESIGN_SCHOOL,hqLoc.add(Direction.SOUTHWEST))){
                    isSchool=true;
                    schLoc=hqLoc.add(Direction.SOUTHWEST);
                }
            }else if(!isCenter){
                if(tryBuild(RobotType.FULFILLMENT_CENTER,hqLoc.add(Direction.NORTHEAST))){
                    isCenter=true;
                }
                if(tryBuild(RobotType.FULFILLMENT_CENTER,hqLoc.add(Direction.NORTHWEST))){
                    isCenter=true;
                }
                if(tryBuild(RobotType.FULFILLMENT_CENTER,hqLoc.add(Direction.SOUTHEAST))){
                    isCenter=true;
                }
                if(tryBuild(RobotType.FULFILLMENT_CENTER,hqLoc.add(Direction.SOUTHWEST))){
                    isCenter=true;
                }
            }else if(vapeCount<=2){
                if(tryBuild(RobotType.VAPORATOR,hqLoc.add(Direction.NORTHEAST))){
                    vapeCount++;
                }
                if(tryBuild(RobotType.VAPORATOR,hqLoc.add(Direction.NORTHWEST))){
                    vapeCount++;
                }
                if(tryBuild(RobotType.VAPORATOR,hqLoc.add(Direction.SOUTHEAST))){
                    vapeCount++;
                }
                if(tryBuild(RobotType.VAPORATOR,hqLoc.add(Direction.SOUTHWEST))){
                    vapeCount++;
                }
            }else if(firstNetGun==null){
                if(tryBuild(RobotType.NET_GUN,hqLoc.add(hqLoc.directionTo(schLoc).opposite().rotateRight()))){
                    firstNetGun=hqLoc.add(hqLoc.directionTo(schLoc).opposite().rotateRight());
                }
                if(tryBuild(RobotType.NET_GUN,hqLoc.add(hqLoc.directionTo(schLoc).opposite().rotateLeft()))){
                    firstNetGun=hqLoc.add(hqLoc.directionTo(schLoc).opposite().rotateLeft());
                }
            }else{
                if(tryBuild(RobotType.NET_GUN,hqLoc.add(hqLoc.directionTo(firstNetGun).rotateRight().rotateRight()))){
                    rc.disintegrate();
                }
                if(tryBuild(RobotType.NET_GUN,hqLoc.add(hqLoc.directionTo(firstNetGun).rotateLeft().rotateLeft()))){
                    rc.disintegrate();
                }
            }
            swarmTo(hqLoc);

        //ENDGAME
        } else if(endGame && rc.getLocation().isAdjacentTo(hqLoc)){
            swarmTo(hqLoc);
        }else if(onWaffle(rc.getLocation())) {
            //build vapes on waffle
            tryBuildOnGrid(bestBuilding());
            waffleWalk(randomDirection());
        }else{
            if (endGame && schoolCount < 2) {
                tryBuild(RobotType.DESIGN_SCHOOL, randomDirection());
            }
            //REGULAR CODE
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

            MapLocation[] soups = rc.senseNearbySoup();
            MapLocation closestLoc = null;
            if (soups.length > 0) {
                closestLoc = soups[0];
            }

            for (MapLocation loc : soups) {
                if (rc.getLocation().distanceSquaredTo(loc) < rc.getLocation().distanceSquaredTo(closestLoc)) {
                    closestLoc = loc;

                }
            }
            //When to build refinery
            if (soups.length > 5 && rc.getLocation().distanceSquaredTo(hqLoc) > 81 && (closestRefinery() == null || (closestRefinery() != null && rc.getLocation().distanceSquaredTo(closestRefinery()) > 81))) {
                tryBuild(RobotType.REFINERY, dirTo(closestLoc).opposite());
            }
            //when to urgently build a refinery
            if (rc.getRoundNum() > turtleRound && closestRefinery() == null && rc.getLocation().distanceSquaredTo(hqLoc) > 16) {
                tryBuild(RobotType.REFINERY, randomDirection());
            }
            if (rc.getSoupCarrying() == RobotType.MINER.soupLimit) {
                if (refLoc.size() > 0) {
                    bugNav(closestRefinery());
                } else {
                    bugNav(hqLoc);
                }
                exploring = false;
            } else if (closestLoc != null) {
                bugNav(closestLoc);
                exploring = false;
            } else if (lastSoup != null) {
                bugNav(lastSoup);
                if (rc.getLocation().isWithinDistanceSquared(lastSoup, 9)) {
                    lastSoup = null;
                }
                exploring = false;
            } else if (exploring) {
                if (rc.getLocation().isWithinDistanceSquared(explore[exploreIndex], 4)) {
                    exploring = false;
                }
                bugNav(explore[exploreIndex]);
            } else if (!exploring) {
                exploring = true;
                exploreIndex = (int) ((explore.length - 1) * Math.random());
                bugNav(explore[exploreIndex]);
            }
        }
    }


    static void runRefinery() throws GameActionException {

        // System.out.println("Pollution: " + rc.sensePollution(rc.getLocation()));
    }

    static void runVaporator() throws GameActionException {
    }

    static void runDesignSchool() throws GameActionException {
        chainScan();
        if(rushFactory || rc.getLocation().isAdjacentTo(hqLoc)) {
            scanRefinery();
            chainScan();
            landscaperCount = 0;
            boolean robotNotOnWall = false;
            boolean isDroneInWall = false;
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.LANDSCAPER && robot.team == rc.getTeam()) {
                    landscaperCount++;
                }
                if (robot.getType() == RobotType.LANDSCAPER && robot.team == rc.getTeam() && !onWall(robot.location)) {
                    robotNotOnWall = true;
                }
            }
            for(Direction dir : directions){
                if(rc.senseRobotAtLocation(hqLoc.add(dir)) != null && rc.senseRobotAtLocation(hqLoc.add(dir)).type == RobotType.DELIVERY_DRONE){
                    isDroneInWall=true;
                }
            }
            System.out.println(landscaperCount);
            if (rushFactory) {
                System.out.println("I rush");
                if (landscaperCount < 4 && (rc.getRoundNum() < turtleRound || rc.getTeamSoup() > 210)) {
                    if (tryBuild(RobotType.LANDSCAPER, dirTo(enemyHQ))) {
                        landscaperCount++;
                    }
                }
            } else if (rc.getLocation().isAdjacentTo(hqLoc)) {
                if (landscaperCount < 3 && (closestRefinery() != null || rc.getTeamSoup() > 200)) {
                    if (tryBuild(RobotType.LANDSCAPER, dirTo(hqLoc).opposite())) {
                        landscaperCount++;
                    }
                } else if (isDroneInWall && landscaperCount < 4 && !robotNotOnWall && (rc.getTeamSoup() > 510 || vapeCount >= 2)) {
                    if (tryBuild(RobotType.LANDSCAPER, dirTo(hqLoc).opposite())) {
                        landscaperCount++;
                    }
                }else if(landscaperCount<18 && (accumulate() || rc.getTeamSoup()>600)){
                    if(tryBuild(RobotType.LANDSCAPER, dirTo(hqLoc).opposite())) {
                        landscaperCount++;
                        startAccum();
                    }
                }
            }
        }else{
            if(landscaperCount<1){
                if(tryBuild(RobotType.LANDSCAPER, dirTo(hqLoc).opposite())) {
                    landscaperCount++;
                    startAccum();
                }
            }
            if(accumulate() && landscaperCount<7){
                if(tryBuild(RobotType.LANDSCAPER, dirTo(hqLoc).opposite())) {
                    landscaperCount++;
                    startAccum();
                }
            }
        }



////        Build a landscaper in the closest possible direction to the HQ
//        int landscaperLimit = 9; // This is a temporary landscaper limit.
//
//        if (landscaperCount < landscaperLimit) {
//            if (tryBuild(RobotType.LANDSCAPER, dirTo(hqLoc))) {
//                landscaperCount++;
//            }
//        }
    }

    static void runFulfillmentCenter() throws GameActionException {
        chainScan();
        if(rc.getLocation().isAdjacentTo(hqLoc)) {
            int droneLimit = 1; // This is a temporary drone limit.
            if (droneCount < droneLimit && rc.getTeamSoup() > 200) {
                if (tryBuild(RobotType.DELIVERY_DRONE, rc.getLocation().add(dirTo(hqLoc).rotateRight()))) {
                    droneCount++;
                } else if (tryBuild(RobotType.DELIVERY_DRONE, rc.getLocation().add(dirTo(hqLoc).rotateLeft()))) {
                    droneCount++;
                }
            }
        }else{
            if(droneCount<1){
                if(tryBuild(RobotType.DELIVERY_DRONE, dirTo(hqLoc).opposite())) {
                    droneCount++;
                    startAccum();
                }
            }
            if(accumulate()){
                if(tryBuild(RobotType.DELIVERY_DRONE, dirTo(hqLoc).opposite())) {
                    droneCount++;
                    startAccum();
                }
            }
        }
    }

    static void runLandscaper() throws GameActionException {
        chainScan();
        System.out.println(protectScaper);
        if(rushScaper){
            if(!rc.getLocation().isAdjacentTo(enemyHQ)){
                moveTowards(enemyHQ);
            }
//            tryDropDirt(enemyHQ);
//            tryDigDirt(Direction.CENTER);
        //CODE FOR WALL BUILDERS
        }else if(onWall(rc.getLocation())){
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.DESIGN_SCHOOL && robot.team != rc.getTeam() && robot.location.isAdjacentTo(rc.getLocation())) {
                    tryDropDirt(robot.location);
                    tryDigDirt(dirTo(hqLoc).opposite());
                }
            }
            int bigNum = 1000000;
            int frontTile = bigNum;
            int rearTile = bigNum;
            int myTile = trySenseElevation(rc.getLocation());
            MapLocation bestTile = rc.getLocation();
            int lowest = trySenseElevation(rc.getLocation());
            MapLocation bestDigTile = null;
            int highest = trySenseElevation(hqLoc);
            for(Direction dir : directions){
                if(onWall(rc.getLocation().add(dir)) && sameAxis(dir)){
                    if(trySenseElevation(rc.getLocation().add(dir))<lowest){
                        bestTile=rc.getLocation().add(dir);
                    }
                    if(frontTile==bigNum){
                        frontTile=trySenseElevation(rc.getLocation().add(dir));
                    }else{
                        rearTile=trySenseElevation(rc.getLocation().add(dir));
                    }
                }
                if(rc.getLocation().add(dir).isAdjacentTo(hqLoc)){
                    if(rc.onTheMap(rc.getLocation().add(dir)) && trySenseElevation(rc.getLocation().add(dir))>highest && (rc.senseRobotAtLocation(rc.getLocation().add(dir))==null || rc.senseRobotAtLocation(rc.getLocation().add(dir)).type==RobotType.DELIVERY_DRONE)){
                        highest = trySenseElevation(rc.getLocation().add(dir));
                        bestDigTile = rc.getLocation().add(dir);
                    }
                }
            }

            if(rc.getRoundNum()%3!=0) {
                tryDropDirt(bestTile);
                if(bestDigTile!=null){
                    tryDigDirt(bestDigTile);
                }else {
                    tryDigDirt(dirTo(hqLoc).opposite());
                }
            }
            wallRun(hqLoc);
            tryDropDirt(bestTile);
            if(bestDigTile!=null){
                tryDigDirt(bestDigTile);
            }else {
                tryDigDirt(dirTo(hqLoc).opposite());
            }
        //CODE FOR PEOPLE TRYING TO GET ONTO THE WALL
        }else if(rc.getLocation().isAdjacentTo(hqLoc)){
            if(rc.senseRobotAtLocation(hqLoc).getDirtCarrying()>0){
                tryDigDirt(hqLoc);
            }
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.DESIGN_SCHOOL && robot.team != rc.getTeam() && robot.location.isAdjacentTo(rc.getLocation())) {
                    tryDropDirt(robot.location);
                    tryDigDirt(dirTo(hqLoc).opposite());
                }
            }
            getOnWall(hqLoc);
            swarmTo(hqLoc);

        //WAFFLE
        }else{
            if (rc.getLocation().isWithinDistanceSquared(targets[currentTarget],rc.getCurrentSensorRadiusSquared()-2) && currentTarget<2) {
                currentTarget += 1;
            }
            if (rc.getLocation().isWithinDistanceSquared(explore[exploreIndex],rc.getCurrentSensorRadiusSquared()-10) && exploreIndex<explore.length-1) {
                exploreIndex = (int)(Math.random()*(explore.length-1));
            }
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.HQ && robot.team != rc.getTeam()) {
                    enemyHQ=robot.location;
                    enemyHQKnown=true;
                    if(!messageSent && trySendChain("420", robot.location.x, robot.location.y)){
                        messageSent = true;
                    }
                }
            }

            if(fillWaffle()){
//                if(protectScaper){
                    moveTowards(explore[exploreIndex]);
//                }else {
//                    if (enemyHQ != null) {
////                        moveTowards(enemyHQ);
////                    }
////                    moveTowards(targets[currentTarget]);
//                }
            }
            tryDigWaffle();
        }



//        if (!layerFilled && rc.canSenseLocation(hqLoc.add(dirTo(hqLoc)))) {
//            boolean empty = false;
//            for (Direction dir : directions) {
//                if ((trySenseRobotAtLocation(hqLoc.add(dir)) == null || trySenseRobotAtLocation(hqLoc.add(dir)).type != RobotType.LANDSCAPER) && rc.canSenseLocation(hqLoc.add(dir))) {
//                    empty = true;
//                }
//            }
//            if (!empty) {
//                layerFilled = true;
//            }
//
//        }
//
//        if (!rc.getLocation().isAdjacentTo(hqLoc)) {
//            moveTowards(hqLoc);
//        }
//
//        if (rc.getLocation().isAdjacentTo(hqLoc) && (layerFilled || rc.getRoundNum() > 350)) {
//            if (tryDigDirt(dirTo(hqLoc).opposite())) {
//                tryDigDirt(dirTo(hqLoc).opposite());
//            }
//            tryDropDirt(rc.getLocation());
//        }
    }

    static void runDeliveryDrone() throws GameActionException {
        chainScan();
        if (endGame) {
            if (rc.getRoundNum() >= 1100 && rc.getRoundNum() < 1150) {
                if (!rc.isCurrentlyHoldingUnit()) {
                    //pick up random landscaper
                    RobotInfo[] robots = rc.senseNearbyRobots();
                    for (RobotInfo robot : robots) {
                        if (robot.type == RobotType.LANDSCAPER && robot.team == rc.getTeam() && (!onWall(robot.location) || !robot.location.isAdjacentTo(hqLoc))) {
                            if (!tryPickUp(robot.ID)) {
                                droneMoveTowards(robot.getLocation());
                            }
                        }
                    }
                } else if (enemyHQ != null) {
                    droneMoveTowards(enemyHQ);
                    isAlreadyHoldingUnit = true;
                }
            } else if (rc.getRoundNum() >= 1200 && rc.getRoundNum() < 1400) {
                droneSwarmAround(enemyHQ);
            } else if (rc.getRoundNum() >= 1400) {
                RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), rc.getTeam().opponent());
                if (robots.length > 0 && !isAlreadyHoldingUnit) {
                    boolean noLandscapers = true;
                    int closestID = 0;
                    int closestDistance = 1000;
                    //run through all robots
                    for (RobotInfo robot : robots) {
                        //only run if the robot found is a landscaper
                        if (robot.getType() == RobotType.LANDSCAPER) {
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
                } else if (isAlreadyHoldingUnit) {
                    if (radiusTo(enemyHQ) > 8) {
                        droneMoveTowards(enemyHQ);
                    } else if (rc.canDropUnit(dirTo(enemyHQ))) {
                        rc.dropUnit(dirTo(enemyHQ));
                    } else {
                        droneMoveTowards(enemyHQ);
                    }
                }
            }
        }
        if(isChosenOne) {
            if (endGame && scapersDisplaced < 2) {
                if (!rc.isCurrentlyHoldingUnit()) {
                    //pick up random landscaper
                    RobotInfo[] robots = rc.senseNearbyRobots();
                    for (RobotInfo robot : robots) {
                        if (robot.type == RobotType.LANDSCAPER && robot.team == rc.getTeam() && (onWall(robot.location) || robot.location.isAdjacentTo(hqLoc))) {
                            tryPickUp(robot.ID);
                        }
                    }
                    droneMoveTowards(hqLoc);
                } else {
                    if (rc.getLocation().distanceSquaredTo(hqLoc) > 25) {
                        for (Direction dir : directions) {
                            if (rc.canDropUnit(dir) && tilePassable(rc.getLocation().add(dir)) && trySenseElevation(rc.getLocation().add(dir)) <= waffleHeight && !waffleHole(rc.getLocation().add(dir))) {
                                rc.dropUnit(dir);
                                scapersDisplaced++;
                                break;
                            }
                        }
                    }
                    droneMoveTowards(targets[currentTarget]);
                }
            } else if (endGame && minersDisplaced < 3) {
                if (!rc.isCurrentlyHoldingUnit()) {
                    //pick up random landscaper
                    RobotInfo[] robots = rc.senseNearbyRobots();
                    for (RobotInfo robot : robots) {
                        if (robot.type == RobotType.MINER && robot.team == rc.getTeam() && robot.location.isAdjacentTo(hqLoc)) {
                            tryPickUp(robot.ID);
                        }
                    }
                    droneMoveTowards(hqLoc);
                } else {
                    if (rc.getLocation().distanceSquaredTo(hqLoc) > 20 && rc.getLocation().distanceSquaredTo(hqLoc) < 49) {
                        for (Direction dir : directions) {
                            if (rc.canDropUnit(dir) && tilePassable(rc.getLocation().add(dir)) && onWaffle(rc.getLocation().add(dir))) {
                                rc.dropUnit(dir);
                                minersDisplaced++;
                                break;
                            }
                        }
                    }
                    if (droneSwarmAround(hqLoc)) {
                    } else {
                        droneMoveTowards(targets[currentTarget]);
                    }
                }
            } else {
                if (!rc.isCurrentlyHoldingUnit()) {
                    RobotInfo[] robots = rc.senseNearbyRobots();
                    for (RobotInfo robot : robots) {
                        System.out.println(!onWall(robot.location));
                        if (robot.type == RobotType.LANDSCAPER && robot.team == rc.getTeam() && robot.location.isAdjacentTo(hqLoc)) {
                            System.out.println("I want to pick up" + Integer.toString(robot.ID));
                            tryPickUp(robot.ID);
                        }
                    }
                    swarmTo(hqLoc);
                } else {
                    for (Direction dir : directions) {
                        if (rc.canDropUnit(dir) && onWall(rc.getLocation().add(dir))) {
                            rc.dropUnit(dir);
                            break;
                        }
                    }
                    swarmTo(hqLoc);
                }
            }
        }else{
            if (rc.getLocation().isWithinDistanceSquared(targets[currentTarget],rc.getCurrentSensorRadiusSquared()) && currentTarget<2) {
                currentTarget += 1;
            }
            RobotInfo[] robots = rc.senseNearbyRobots();
            for (RobotInfo robot : robots) {
                if (robot.getType() == RobotType.HQ && robot.team != rc.getTeam()) {
                    enemyHQ=robot.location;
                    enemyHQKnown=true;
                    if(!messageSent && trySendChain("420", robot.location.x, robot.location.y)){
                        messageSent = true;
                    }
                }
            }
            if(enemyHQ!=null){
                droneSwarmAround(enemyHQ);
            }
            droneSwarmAround(targets[currentTarget]);
        }



//        Team enemy = rc.getTeam().opponent();
//        MapLocation[] targets = new MapLocation[]{new MapLocation(rc.getMapWidth() - hqLoc.x, rc.getMapHeight() - hqLoc.y),
//            new MapLocation(rc.getMapWidth() - hqLoc.x, hqLoc.y),
//            new MapLocation(hqLoc.x, rc.getMapHeight() - hqLoc.y)};
//        if (isChosenOne && rc.getRoundNum() > 750) {
//            droneMoveTowards(targets[currentTarget]);
//            if (rc.getLocation().equals(targets[currentTarget])) {
//                currentTarget += 1;
//            }
//            RobotInfo[] robots = rc.senseNearbyRobots();
//            for (RobotInfo robot : robots) {
//                if (robot.getType() == RobotType.HQ && robot.team == enemy) {
//                    if (trySendChain("420", robot.location.x, robot.location.y)) {
//                        isChosenOne = false;
//                    }
//
//                }
//            }
//        } else if (isChosenOne) {
//            droneMoveTowards(mapMid);
//        }
//        System.out.println(!isChosenOne && rc.getRoundNum() > 750 && enemyHQKnown);
//        // wait till round 750 and until you know where enemy hq is
//        if (!isChosenOne && rc.getRoundNum() > 750 && enemyHQKnown) {
//            // run if you havent captured a landscaper yet
//            if (!rc.isCurrentlyHoldingUnit()) {
//                //scan for robots
//                RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), enemy);
//                //if any robots were found and round is above 1000
//                if (robots.length > 0 && rc.getRoundNum() > 1000) {
//                    boolean noLandscapers = true;
//                    int closestID = 0;
//                    int closestDistance = 1000;
//                    //run through all robots
//                    for (RobotInfo robot : robots) {
//                        //only run if the robot found is a landscaper
//                        if (robot.getType() == RobotType.LANDSCAPER && robot.team == enemy) {
//                            noLandscapers = false;
//                            //if you can pick it up please do so
//                            if (rc.canPickUpUnit(robot.getID())) {
//                                rc.pickUpUnit(robot.getID());
//                                break;
//                            }
//                            //find the closest landscaper
//                            if (rc.getLocation().distanceSquaredTo(robot.location) < closestDistance) {
//                                closestDistance = rc.getLocation().distanceSquaredTo(robot.location);
//                                closestID = robot.ID;
//                            }
//                        }
//                    }
//                    // go to the closest landscaper
//                    if (closestID != 0) {
//                        droneMoveTowards(rc.senseRobot(closestID).location);
//                    }
//                    //if you couldn't find any landscapers go to the enemyHQ
//                    if (noLandscapers) {
//                        droneSwarmAround(enemyHQ);
//                    }
//                    // if no robots found swarm around enemy HQ
//                } else {
//                    droneSwarmAround(enemyHQ);
//                }
//                // if robot picked up the run the hell away
//            } else {
//                aboveUnit = new MapLocation(rc.getLocation().x, rc.getLocation().y + 1);
//                belowUnit = new MapLocation(rc.getLocation().x, rc.getLocation().y - 1);
//                if (rc.senseFlooding(aboveUnit) && rc.canDropUnit(dirTo(aboveUnit))) {
//                    rc.dropUnit(Direction.NORTH);
//                } else if (rc.senseFlooding(belowUnit) && rc.canDropUnit(dirTo(belowUnit))) {
//                    rc.dropUnit(Direction.SOUTH);
//                } else {
//                    droneMoveTowards(dirTo(enemyHQ).opposite());
//                }
//            }
//            // Huddle in the middle till the time comes
//        } else if (!isChosenOne) {
//            RobotInfo[] robots = rc.senseNearbyRobots(rc.getCurrentSensorRadiusSquared(), enemy);
//            System.out.println(robots.length);
//            if (robots.length > 0) {
//                for (RobotInfo robot : robots) {
//                    if (robot.getType() == RobotType.MINER || robot.getType() == RobotType.LANDSCAPER) {
//                        if (rc.canPickUpUnit(robot.getID())) {
//                            rc.pickUpUnit(robot.getID());
//                            break;
//                        } else {
//                            droneMoveTowards(rc.senseRobot(robot.getID()).location);
//                        }
//                    }
//                }
//            } else {
//                if(notChosenCurrentTarget==0) {
//                    droneSwarmAround(mapMid);
//                    if(rc.getLocation().isWithinDistanceSquared(mapMid,RobotType.NET_GUN.sensorRadiusSquared+5)){
//                        notChosenCurrentTarget=1;
//                    }
//                }if(notChosenCurrentTarget==1){
//                    droneSwarmAround(hqLoc);
//                }
//
//            }
//        }

    }



    static void runNetGun() throws GameActionException {
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.DELIVERY_DRONE && rc.canShootUnit(robot.getID()) && robot.team != rc.getTeam()) {
                rc.shootUnit(robot.ID);
            }
        }
    }

    static void startAccum() throws GameActionException{
        accumulation = 0;
    }

    static boolean accumulate() throws GameActionException{
        if(accumulation<=1) {
            accumulation += vapeCount * 0.001;
        }
        if(accumulation>=1){
            return true;
        }
        return false;
    }

    static RobotType bestBuilding() throws GameActionException{
        if(vapeCount<3 && schoolCount<3 && centerCount<2){
            return RobotType.VAPORATOR;
        }
        if(vapeCount>3 && schoolCount<=4 && centerCount<=4){
            return RobotType.DESIGN_SCHOOL;
        }
        if(vapeCount>3 && schoolCount>=4 && centerCount<=4){
            return RobotType.FULFILLMENT_CENTER;
        }
        return RobotType.VAPORATOR;
    }

    static boolean fillWaffle() throws GameActionException{
        int height = waffleHeight;
        MapLocation bestTile = null;
        int lowest = waffleHeight;
        boolean waffleDone = true;
        //Find best directional tile to put dirt on
        for(Direction dir : directions){
            MapLocation scanLoc = rc.getLocation().add(dir);
            if(!waffleHole(scanLoc) && trySenseElevation(scanLoc)<lowest && trySenseElevation(scanLoc)>-100 && scanLoc.distanceSquaredTo(hqLoc)>20){
                lowest = trySenseElevation(scanLoc);
                bestTile = scanLoc;
            }
            if(!waffleHole(scanLoc) && scanLoc.distanceSquaredTo(hqLoc)>25 && ((trySenseElevation(scanLoc)<30 && trySenseElevation(scanLoc)>waffleHeight) || (trySenseElevation(scanLoc)<waffleHeight && trySenseElevation(scanLoc)>-10))){
                waffleDone = false;
            }
        }
        //try Center Location
        MapLocation scanLoc = rc.getLocation();
        if(!waffleHole(scanLoc) && trySenseElevation(scanLoc)<lowest && scanLoc.distanceSquaredTo(hqLoc)>25){
            lowest = trySenseElevation(scanLoc);
            bestTile = scanLoc;
        }

        if(bestTile != null) {
            tryDropDirt(bestTile);
        }
        if(waffleDone){
            return true;
        }
        return false;
    }

    static void tryDigWaffle() throws GameActionException{
        if(rc.getDirtCarrying()==25){
            for(Direction dir : directions){
                MapLocation scanLoc = rc.getLocation().add(dir);
                if(waffleHole(scanLoc)){
                    tryDropDirt(scanLoc);
                }
            }
        }
        MapLocation bestTile = null;
        int highest = -10000;
        for(Direction dir : directions){
            MapLocation scanLoc = rc.getLocation().add(dir);
            if((waffleHole(scanLoc) || (trySenseElevation(scanLoc)>waffleHeight && trySenseElevation(scanLoc)<30)) && trySenseElevation(scanLoc)>highest && !onWall(scanLoc) && rc.senseRobotAtLocation(scanLoc)==null){
                highest = trySenseElevation(scanLoc);
                bestTile = scanLoc;
            }
        }
        if(bestTile != null) {
            tryDigDirt(bestTile);
        }else{
            moveTowards(randomDirection());
        }
    }

    static boolean waffleHole(MapLocation loc) throws GameActionException{
        if(rc.onTheMap(loc) && !onWall(loc) && loc.x % 2 == 0 && loc.y % 2 == 0){
            return true;
        }
        return false;
    }

    static boolean onWaffle(MapLocation loc) throws GameActionException{
        if(trySenseElevation(loc)>= waffleHeight && trySenseElevation(loc)<= waffleHeight + 3){
            return true;
        }
        return false;
    }

    static boolean onBuildGrid(MapLocation loc) throws GameActionException{
        if(onWaffle(loc) && !onWall(loc) &&  loc.x % 2 != 0 && loc.y % 2 != 0){
            return true;
        }
        return false;
    }

    static boolean tryBuildOnGrid(RobotType type) throws GameActionException{
        MapLocation myLoc = rc.getLocation();
        for (Direction dir : directions){
            MapLocation scanLoc = myLoc.add(dir);
            if(onBuildGrid(scanLoc)){
                if(tryHardBuild(type,dir)){
                    return true;
                }
            }
        }return false;
    }

    static boolean onWall(MapLocation loc) throws GameActionException{
        boolean onWall = false;
        if(rc.onTheMap(loc) && ((Math.abs(loc.x-hqLoc.x)==2 && Math.abs(loc.y-hqLoc.y)<=2) || (Math.abs(loc.y-hqLoc.y)==2 && Math.abs(loc.x-hqLoc.x)<=2))){
            onWall = true;
        }
        return onWall;
    }

    static boolean waffleWalk(Direction dir) throws GameActionException{
        if(onWaffle(rc.getLocation().add(dir))){
            tryMove(dir);
            return true;
        }else if(onWaffle(rc.getLocation().add(dir.rotateRight()))){
            tryMove(dir.rotateRight());
            return true;
        }else if(onWaffle(rc.getLocation().add(dir.rotateLeft()))){
            tryMove(dir.rotateLeft());
            return true;
        }else if(onWaffle(rc.getLocation().add(dir.rotateRight().rotateRight()))){
            tryMove(dir.rotateRight().rotateRight());
            return true;
        }else if(onWaffle(rc.getLocation().add(dir.rotateLeft().rotateLeft()))){
            tryMove(dir.rotateLeft().rotateLeft());
            return true;
        }
        return false;
    }


        static void bugNav(MapLocation loc) throws GameActionException{
        System.out.println(loc);
        if(rc.isReady()) {
            if (!loc.equals(lastTarget)) {
                closest = rc.getLocation().distanceSquaredTo(loc);
                steps=0;
                lastDirection = dirTo(loc).rotateRight();
            }
            Direction bestDir = Direction.NORTH;
            boolean closer = false;
            for (Direction dir : directions) {
                if (tilePassable(rc.getLocation().add(dir)) && rc.getLocation().add(dir).distanceSquaredTo(loc) < closest) {
                    bestDir = dir;
                    closest = rc.getLocation().add(dir).distanceSquaredTo(loc);
                    closer = true;
                }
            }
            if (closer) {
                tryMove(bestDir);
            } else {
                Direction tryDirection = lastDirection.rotateLeft().rotateLeft();
                if(!tilePassable(rc.getLocation().add(lastDirection.rotateLeft()))){
                    tryDirection = lastDirection.rotateLeft();
                }else if(!tilePassable(rc.getLocation().add(lastDirection.rotateLeft().rotateLeft()))){
                    tryDirection = lastDirection.rotateLeft().rotateLeft();
                }
//                Direction tryDirection2 = dirTo(loc);
//                for(Direction dir : directions){
//                    if(!tilePassable(rc.getLocation().add(tryDirection2))){
//                        tryDirection = tryDirection2;
//                        break;
//                    }
//                    tryDirection2 = tryDirection2.rotateRight();
//                }
//                if(tryDirection==null){
//                    tryMove(dirTo(loc));
//                }else {
                boolean moved = false;
                for (Direction dir : directions) {
                    if (tryMove(tryDirection)) {
                        lastDirection = tryDirection;
                        moved = true;
                        break;
                    }
                    tryDirection = tryDirection.rotateRight();
                }
                if(!moved){
                    tryMove(dirTo(loc));
                }
            }
//            }
            steps++;
            lastTarget = loc;
        }
    }

    static void maybeDie() throws GameActionException{
        if(rc.getRoundNum()>250 && rc.getRoundNum()<255 && (rc.getLocation().isAdjacentTo(hqLoc) || onWall(rc.getLocation())) && !turtleMiner){
            rc.disintegrate();
        }
        if(rc.getRoundNum()>250 && onWall(rc.getLocation())){
            rc.disintegrate();
        }
    }

    static void scanRefinery() throws GameActionException{
        RobotInfo[] robots = rc.senseNearbyRobots();
        for (RobotInfo robot : robots) {
            if (robot.getType() == RobotType.REFINERY && robot.team == rc.getTeam()) {
                boolean alreadyAdded = false;
                for(MapLocation loc : refLoc){
                    if (robot.location == loc){
                        alreadyAdded = true;
                        break;
                    }
                }
                if(!alreadyAdded){
                    refLoc.add(robot.location);
                }
            }
        }
    }

    static MapLocation closestRefinery() throws GameActionException {
        int closest = 100000;
        MapLocation closestRef = null;
        for(MapLocation loc : refLoc){
            if(rc.getLocation().distanceSquaredTo(loc)<closest){
                closestRef = loc;
                closest = rc.getLocation().distanceSquaredTo(loc);
            }
        }
        if(closestRef !=null){
            return closestRef;
        }
        return null;
    }

    static boolean tilePassable(MapLocation loc) throws GameActionException{
        if(rc.canMove(dirTo(loc)) && !rc.senseFlooding(loc)){
            return true;
        }
        return false;
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

    static void swarmTo(MapLocation loc) throws GameActionException{
        if(rc.getRoundNum()%6<3) {
            if (rc.getLocation().isAdjacentTo(loc)) {
                Direction dir = dirTo(loc);
                if (rc.getLocation().add(dir).isAdjacentTo(loc) && tilePassable(rc.getLocation().add(dir))) {
                    tryMove(dir);
                } else if (rc.getLocation().add(dir.rotateRight()).isAdjacentTo(loc) && tilePassable(rc.getLocation().add(dir.rotateRight()))) {
                    tryMove(dir.rotateRight());
                } else if (rc.getLocation().add(dir.rotateRight().rotateRight()).isAdjacentTo(loc) && tilePassable(rc.getLocation().add(dir.rotateRight().rotateRight()))) {
                    tryMove(dir.rotateRight().rotateRight());
                } else if (rc.getLocation().add(dir.rotateLeft()).isAdjacentTo(loc) && tilePassable(rc.getLocation().add(dir.rotateLeft()))) {
                    tryMove(dir.rotateLeft());
                } else if (rc.getLocation().add(dir.rotateLeft().rotateLeft()).isAdjacentTo(loc) && tilePassable(rc.getLocation().add(dir.rotateLeft().rotateLeft()))) {
                    tryMove(dir.rotateLeft().rotateLeft());
                }
            } else {
                bugNav(loc);
            }
        }else{
            if (rc.getLocation().isAdjacentTo(loc)) {
                Direction dir = dirTo(loc);
                if (rc.getLocation().add(dir).isAdjacentTo(loc) && tilePassable(rc.getLocation().add(dir))) {
                    tryMove(dir);
                }  else if (rc.getLocation().add(dir.rotateLeft()).isAdjacentTo(loc) && tilePassable(rc.getLocation().add(dir.rotateLeft()))) {
                    tryMove(dir.rotateLeft());
                } else if (rc.getLocation().add(dir.rotateLeft().rotateLeft()).isAdjacentTo(loc) && tilePassable(rc.getLocation().add(dir.rotateLeft().rotateLeft()))) {
                    tryMove(dir.rotateLeft().rotateLeft());
                } else if (rc.getLocation().add(dir.rotateRight()).isAdjacentTo(loc) && tilePassable(rc.getLocation().add(dir.rotateRight()))) {
                    tryMove(dir.rotateRight());
                } else if (rc.getLocation().add(dir.rotateRight().rotateRight()).isAdjacentTo(loc) && tilePassable(rc.getLocation().add(dir.rotateRight().rotateRight()))) {
                    tryMove(dir.rotateRight().rotateRight());
                }
            } else {
                bugNav(loc);
            }
        }
    }

    static void getOnWall(MapLocation loc) throws GameActionException{
        for(Direction dir : directions){
            if(onWall(rc.getLocation().add(dir))){
                tryMove(dir);
            }
        }
    }

    static void wallRun(MapLocation loc) throws GameActionException{
        Direction dir = dirTo(loc);
        if(rc.getRoundNum()%150>75) {
            if (onWall(rc.getLocation().add(dir)) && tilePassable(rc.getLocation().add(dir)) && sameAxis(dir)) {
                tryMove(dir);
            } else if (onWall(rc.getLocation().add(dir.rotateRight())) && tilePassable(rc.getLocation().add(dir.rotateRight())) && sameAxis(dir.rotateRight())) {
                tryMove(dir.rotateRight());
            } else if (onWall(rc.getLocation().add(dir.rotateRight().rotateRight())) && tilePassable(rc.getLocation().add(dir.rotateRight().rotateRight())) && sameAxis(dir.rotateRight().rotateRight())) {
                tryMove(dir.rotateRight().rotateRight());
            } else if (onWall(rc.getLocation().add(dir.rotateRight().rotateRight().rotateRight())) && tilePassable(rc.getLocation().add(dir.rotateRight().rotateRight().rotateRight())) && sameAxis(dir.rotateRight().rotateRight().rotateRight())) {
                tryMove(dir.rotateRight().rotateRight().rotateRight());
            } else if (onWall(rc.getLocation().add(dir.rotateLeft())) && tilePassable(rc.getLocation().add(dir.rotateLeft())) && sameAxis(dir.rotateLeft())) {
                tryMove(dir.rotateLeft());
            } else if (onWall(rc.getLocation().add(dir.rotateLeft().rotateLeft())) && tilePassable(rc.getLocation().add(dir.rotateLeft().rotateLeft())) && sameAxis(dir.rotateLeft().rotateLeft())) {
                tryMove(dir.rotateLeft().rotateLeft());
            } else if (onWall(rc.getLocation().add(dir.rotateLeft().rotateLeft().rotateLeft())) && tilePassable(rc.getLocation().add(dir.rotateLeft().rotateLeft().rotateLeft())) && sameAxis(dir.rotateLeft().rotateLeft().rotateLeft())) {
                tryMove(dir.rotateLeft().rotateLeft().rotateLeft());
            }
        }else{
            if (onWall(rc.getLocation().add(dir)) && tilePassable(rc.getLocation().add(dir)) && sameAxis(dir)) {
                tryMove(dir);
            }  else if (onWall(rc.getLocation().add(dir.rotateLeft())) && tilePassable(rc.getLocation().add(dir.rotateLeft())) && sameAxis(dir.rotateLeft())) {
                tryMove(dir.rotateLeft());
            } else if (onWall(rc.getLocation().add(dir.rotateLeft().rotateLeft())) && tilePassable(rc.getLocation().add(dir.rotateLeft().rotateLeft())) && sameAxis(dir.rotateLeft().rotateLeft())) {
                tryMove(dir.rotateLeft().rotateLeft());
            } else if (onWall(rc.getLocation().add(dir.rotateLeft().rotateLeft().rotateLeft())) && tilePassable(rc.getLocation().add(dir.rotateLeft().rotateLeft().rotateLeft())) && sameAxis(dir.rotateLeft().rotateLeft().rotateLeft())) {
                tryMove(dir.rotateLeft().rotateLeft().rotateLeft());
            } else if (onWall(rc.getLocation().add(dir.rotateRight())) && tilePassable(rc.getLocation().add(dir.rotateRight())) && sameAxis(dir.rotateRight())) {
                tryMove(dir.rotateRight());
            } else if (onWall(rc.getLocation().add(dir.rotateRight().rotateRight())) && tilePassable(rc.getLocation().add(dir.rotateRight().rotateRight())) && sameAxis(dir.rotateRight().rotateRight())) {
                tryMove(dir.rotateRight().rotateRight());
            } else if (onWall(rc.getLocation().add(dir.rotateRight().rotateRight().rotateRight())) && tilePassable(rc.getLocation().add(dir.rotateRight().rotateRight().rotateRight())) && sameAxis(dir.rotateRight().rotateRight().rotateRight())) {
                tryMove(dir.rotateRight().rotateRight().rotateRight());
            }
        }
    }
    static boolean sameAxis(Direction dir) throws GameActionException{
        if(rc.getLocation().x==rc.getLocation().add(dir).x || rc.getLocation().y==rc.getLocation().add(dir).y){
            return true;
        }return false;
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
        int radius = RobotType.NET_GUN.sensorRadiusSquared + 5;
        if(rc.getRoundNum() % 100 < 50) {
            if (!rc.getLocation().add(dirTo(loc)).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc))) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateRight()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateRight())) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateRight().rotateRight()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateRight().rotateRight())) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateLeft()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateLeft())) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateLeft().rotateLeft()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateLeft().rotateLeft())) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateRight().rotateRight().rotateRight()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateRight().rotateRight().rotateRight())) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateLeft().rotateLeft().rotateLeft()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateLeft().rotateLeft().rotateLeft())) {
                    return true;
                }
            }
        }else{
            if (!rc.getLocation().add(dirTo(loc)).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc))) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateLeft()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateLeft())) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateLeft().rotateLeft()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateLeft().rotateLeft())) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateRight()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateRight())) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateRight().rotateRight()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateRight().rotateRight())) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateLeft().rotateLeft().rotateLeft()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateLeft().rotateLeft().rotateLeft())) {
                    return true;
                }
            }
            if (!rc.getLocation().add(dirTo(loc).rotateRight().rotateRight().rotateRight()).isWithinDistanceSquared(loc, radius)) {
                if (tryDroneMove(dirTo(loc).rotateRight().rotateRight().rotateRight())) {
                    return true;
                }
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

    static boolean tryPickUp(int id) throws GameActionException {
        if (rc.canPickUpUnit(id)) {
            rc.pickUpUnit(id);
            return true;
        } else {
            return false;
        }
    }

    static boolean tryHardBuild(RobotType type, Direction dir) throws GameActionException {
        if (rc.canBuildRobot(type, dir)) {
            rc.buildRobot(type, dir);
            return true;
        }
        return false;
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

    static boolean tryDigDirt(MapLocation loc) throws GameActionException {
        if (rc.isReady() && rc.canDigDirt(dirTo(loc)) && (rc.getLocation().isAdjacentTo(loc) || rc.getLocation() == loc)) {
            rc.digDirt(dirTo(loc));
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
        String falseMsg = 34 + String.format("%05d", (int) (Math.random() * 10000));
        int[] trans = {
            0,
            0,
            0,
            0,
            0,
            0,
            0
        };
        if (chainType.equals("69") || chainType.equals("96") || chainType.equals("11")) {
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
        String falseMsg = 34 + String.format("%05d", (int) (Math.random() * 10000));
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
        //We're in the endgame now
        if (chainType.equals("116")) {
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

    static void masterChainSend() throws GameActionException {
        String message = null;
        String falseMsg = 34 + String.format("%05d", (int) (Math.random() * 10000));
        int[] trans = {
                0,
                0,
                0,
                0,
                0,
                0,
                0
        };

        trans[0] = 5877033;
        trans[1] = schoolCount;
        trans[2] = centerCount;
        trans[3] = vapeCount;
        if(enemyHQKnown) {
            message = 723 + String.format("%02d", enemyHQ.x) + String.format("%02d", enemyHQ.y);
            trans[4] = Integer.parseInt(message);
        }else{
            falseMsg = 25 + String.format("%05d", (int) (Math.random() * 100000));
            trans[4] = Integer.parseInt(falseMsg);
        }
        falseMsg = 95 + String.format("%05d", (int) (Math.random() * 100000));
        trans[5] = Integer.parseInt(falseMsg);
        falseMsg = 54 + String.format("%05d", (int) (Math.random() * 100000));
        trans[6] = Integer.parseInt(falseMsg);

        if (rc.canSubmitTransaction(trans, 1)) {
            rc.submitTransaction(trans, 1);
        }
    }


    static void chainScan() throws GameActionException {
        Transaction[] trans = rc.getBlock(rc.getRoundNum() - 1);
        int loop = 0;
        for (Transaction tran : trans) {
            int[] me = tran.getMessage();

            if(me[0] == 5877033 && rc.getType()!=RobotType.HQ){
                schoolCount=me[1];
                centerCount=me[2];
                vapeCount=me[3];
                endGame = true;
                if(Integer.toString(me[4]).length()==7 && Integer.toString(me[loop]).substring(0, 3).equals("723")) {
                    String x = Integer.toString(me[loop]).substring(3, 5);
                    String y = Integer.toString(me[loop]).substring(5, 7);
                    enemyHQ = new MapLocation(Integer.parseInt(x), Integer.parseInt(y));
                }
            }

            if (Integer.toString(me[loop]).length() == 7 && Integer.toString(me[loop+1]).length() == 7) {
                if (Integer.toString(me[loop]).substring(0, 3).equals("774")) {
                    isSchool = true;
                    schoolCount++;
                }
                if (Integer.toString(me[loop]).substring(0, 3).equals("666")) {
                    isCenter = true;
                    centerCount++;
                }
                if (Integer.toString(me[loop]).substring(0, 3).equals("273")) {
                    String x = Integer.toString(me[loop]).substring(3, 5);
                    String y = Integer.toString(me[loop]).substring(5, 7);
                    refLoc.add(new MapLocation(Integer.parseInt(x), Integer.parseInt(y)));
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
                if (Integer.toString(me[loop]).substring(0, 3).equals("116")) {
                    String x = Integer.toString(me[loop]).substring(3, 5);
                    String y = Integer.toString(me[loop]).substring(5, 7);
                    endGame = true;
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
                if (Integer.toString(me[loop]).substring(0, 2).equals("11")) {
                    String id = Integer.toString(me[loop]).substring(2, 7);
                    if (Integer.parseInt(id) == rc.getID()) {
                        turtleMiner = true;
                    }
                }
//                loop++;
            }
        }
    }

    static void hqChainScan() throws GameActionException {
        Transaction[] trans = rc.getBlock(1);
        int loop = 0;
        for (Transaction tran : trans) {
            int[] me = tran.getMessage();
            if(Integer.toString(me[loop]).length() == 7) {
                if (Integer.toString(me[loop]).substring(0, 3).equals("911")) {
                    String x = Integer.toString(me[loop]).substring(3, 5);
                    String y = Integer.toString(me[loop]).substring(5, 7);
                    hqLoc = new MapLocation(Integer.parseInt(x), Integer.parseInt(y));
                }
            }
        }
    }
}
