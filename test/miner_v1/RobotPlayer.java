package version_1;

import battlecode.common.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/**
 * RobotPlayer is the class that describes your main robot strategy.
 * The run() method inside this class is like your main function: this is what we'll call once your robot
 * is created!
 */

public strictfp class RobotPlayer {

    /**
     * We will use this variable to count the number of turns this robot has been alive.
     * You can use static variables like this to save any information you want. Keep in mind that even though
     * these variables are static, in Battlecode they aren't actually shared between your robots.
     */

    static int turnCount = 0;

    static final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    static final Direction[] directions = {
        Direction.NORTH,
        Direction.NORTHEAST,
        Direction.EAST,
        Direction.SOUTHEAST,
        Direction.SOUTH,
        Direction.SOUTHWEST,
        Direction.WEST,
        Direction.NORTHWEST,
    };

    //For wander navigation
    static int[][] rubbleMap; //keeps track of rubble
    static boolean[][] exploredMap; //keeps track of places on the map already been to/sensed
    static int goldMaxThreshold = 1;
    static int goldMinThreshold = 1;
    static int leadMaxThreshold = 50;
    static int leadMinThreshold = 10;
    static int myVision;
    static int myAction;

    public static double exploreFactor = 0.1; //how much the robot will want to explore
    public static int exploreSearchDistance = 5;
    public static double distanceFactor = 0.1; //how much the robot cares about getting closer to its goal
    public static double avoidSameTypeFactor = 5; //how much the robot avoid other robots of its type
    public static double retreatFactor = 1.5; //how much the robot cares about moving away from known enemies (based on damage)
    public static double rubbleFactor = 0.2; //how much robot cares about not stepping on rubble

    //State management
    static boolean mining = false;

    static void initializeMaps(RobotController rc){
        rubbleMap = new int[rc.getMapWidth()][rc.getMapHeight()];
        exploredMap = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        myVision = rc.getType().visionRadiusSquared;
        myAction = rc.getType().actionRadiusSquared;
    }

    //scouts/wandering
    static Direction efficientPath (RobotController rc, MapLocation locus) throws GameActionException{

        try {

            MapLocation m = rc.getLocation();
            RobotInfo[] robots = rc.senseNearbyRobots();

            rc.setIndicatorDot(m.add(Direction.NORTH), 255,0,0);

            for (int dx = - (int) Math.sqrt(myVision); dx <= (int) Math.sqrt(myVision); dx++){
                for (int dy = - (int) Math.sqrt(myVision - Math.pow(dx, 2)); dy <= (int) Math.sqrt(myVision - Math.pow(dx, 2)); dy++){
                    int newX = m.x + dx;
                    int newY = m.y + dy;
                    if (newX >= 0 && newX < rubbleMap.length && newY >= 0 && newY < rubbleMap[newX].length){
                        rubbleMap[newX][newY] = rc.senseRubble(new MapLocation(m.x + dx, m.y + dy));
                        exploredMap[newX][newY] = true;
                    }
                }
            }

            Direction best = null;
            double bestScore = Integer.MIN_VALUE;

            for (int i = 0; i < 8; i++){

                if(!rc.canMove(directions[i])){
                    continue;
                }
                //function = unexplored*k - distance - rubble - enemies
                double score = 0;
                MapLocation locat = m.add(directions[i]);

                score += (m.distanceSquaredTo(locus) - locat.distanceSquaredTo(locus)) * distanceFactor;

                for (int dx = -exploreSearchDistance; dx <= exploreSearchDistance; dx++){
                    for (int dy = -exploreSearchDistance; dy <= exploreSearchDistance; dy++){
                        int newX = m.x + dx;
                        int newY = m.y + dy;
                        if (newX >= 0 && newX < rubbleMap.length && newY >= 0 && newY < rubbleMap[newX].length){
                            if(!exploredMap[newX][newY]){
                                score += exploreFactor;
                            }
                        }
                    }
                }

                score -= rubbleMap[locat.x][locat.y] * rubbleFactor;

                for (RobotInfo robot: robots) {
                    if (robot.team != rc.getTeam() && locat.distanceSquaredTo(robot.location) <= robot.type.actionRadiusSquared) {
                        score -= robot.getType().damage * retreatFactor;
                    }

                    if (robot.team == rc.getTeam() && robot.type == rc.getType() && locat.distanceSquaredTo(robot.location) <= robot.type.visionRadiusSquared){
                        score -= avoidSameTypeFactor;
                    }
                }

                if (score >= bestScore){
                    bestScore = score;
                    best = directions[i];
                }

            }
            if(best != null){
                return best;
            }else{
                return Direction.CENTER;
            }



        } catch (GameActionException e){
            System.out.println("Direction error " + rc.getLocation());
            e.printStackTrace();
            return directions[rng.nextInt(directions.length)];
        }catch ( Exception e){
            e.printStackTrace();
            return directions[rng.nextInt(directions.length)];
        }
    }

    @SuppressWarnings("unused")
    public static void run(RobotController rc) throws GameActionException {

        // Hello world! Standard output is very useful for debugging.
        // Everything you say here will be directly viewable in your terminal when you run a match!
        //System.out.println("I'm a " + rc.getType() + " and I just got created! I have health " + rc.getHealth());
        initializeMaps(rc);
        System.out.println(Arrays.toString(rubbleMap[0]));

        // You can also use indicators to save debug notes in replays.
        //rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            turnCount += 1;  // We have now been alive for one more turn!

            // Try/catch blocks stop unhandled exceptions, which cause your robot to explode.
            try {
                // The same run() function is called for every robot on your team, even if they are
                // different types. Here, we separate the control depending on the RobotType, so we can
                // use different strategies on different robots. If you wish, you are free to rewrite
                // this into a different control structure!
                switch (rc.getType()) {
                    case ARCHON:     runArchon(rc);  break;
                    case MINER:      runMiner(rc);   break;
                    case SOLDIER:    runSoldier(rc); break;
                    case LABORATORY: // Examplefuncsplayer doesn't use any of these robot types below.
                    case WATCHTOWER: // You might want to give them a try!
                    case BUILDER:
                    case SAGE:       break;
                }
            } catch (GameActionException e) {
                // Oh no! It looks like we did something illegal in the Battlecode world. You should
                // handle GameActionExceptions judiciously, in case unexpected events occur in the game
                // world. Remember, uncaught exceptions cause your robot to explode!
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } catch (Exception e) {
                // Oh no! It looks like our code tried to do something bad. This isn't a
                // GameActionException, so it's more likely to be a bug in our code.
                System.out.println(rc.getType() + " Exception");
                e.printStackTrace();

            } finally {
                // Signify we've done everything we want to do, thereby ending our turn.
                // This will make our code wait until the next turn, and then perform this loop again.
                Clock.yield();
            }
            // End of loop: go back to the top. Clock.yield() has ended, so it's time for another turn!
        }

        // Your code should never reach here (unless it's intentional)! Self-destruction imminent...
    }

    /**
     * Run a single turn for an Archon.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runArchon(RobotController rc) throws GameActionException {
        // Pick a direction to build in.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rng.nextBoolean()) {
            // Let's try to build a miner.
            rc.setIndicatorString("Trying to build a miner");
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
            }
        } else {
            // Let's try to build a soldier.
            rc.setIndicatorString("Trying to build a soldier");
            if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                rc.buildRobot(RobotType.SOLDIER, dir);
            }
        }
    }

    /**
     * Run a single turn for a Miner.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runMiner(RobotController rc) throws GameActionException {
        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        int[] arrayInfo = readArray(rc, 9,16);
        rc.setIndicatorString(Arrays.toString(arrayInfo));

        int leadSensed = 0;
        int goldSensed = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                    mining = true;
                }
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                    mining = true;
                }

                if (rc.canSenseLocation(mineLocation)) {
                    leadSensed += rc.senseLead(mineLocation);
                    goldSensed += rc.senseGold(mineLocation);
                }
            }
        }

        if(goldSensed > goldMaxThreshold|| leadSensed > leadMaxThreshold){
            //write to array about mylocation
            WriteLocationToOpenSpace(rc, arrayInfo,9, 16, me, 2);
        }else if(goldSensed < goldMinThreshold && leadSensed < leadMinThreshold){
            //search array for my location and remove it from the array
            DeleteLocation(rc, arrayInfo, 9, 16, me, 2);
            if (goldSensed == 0 && leadSensed == 0){
                mining = false;
            }
        }

        // Also try to move to places not yet explored.
        if(!mining) {

            int dist = Integer.MAX_VALUE;
            ArrayList<MapLocation> goldTargets = new ArrayList<MapLocation>();
            ArrayList<MapLocation> leadTargets = new ArrayList<MapLocation>();
            MapLocation target = null;

            for (int dx = - (int) Math.sqrt(myVision); dx <= (int) Math.sqrt(myVision); dx++) {
                for (int dy = - (int) Math.sqrt(myVision - Math.pow(dx,2)); dy <= (int) Math.sqrt(myVision - Math.pow(dx,2)); dy++) {
                    MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                    if (rc.canSenseLocation(mineLocation)) {
                        int leadSeen = rc.senseLead(mineLocation);
                        int goldSeen = rc.senseGold(mineLocation);

                        if(goldSeen > 0){
                            goldTargets.add(mineLocation);
                        }
                        if(leadSeen > 0){
                            leadTargets.add(mineLocation);
                        }
                    }
                }
            }

            if (goldTargets.isEmpty()){
                if (leadTargets.isEmpty()){
                    //search through 8 array integers for the closest goal
                    target = getClosestLocation(arrayInfo, 9, 16, me);
                    if (target == null){
                        target = new MapLocation(rubbleMap.length / 2, rubbleMap[0].length / 2);
                    }
                }else{
                    //search through lead targets for closest goal
                    for(MapLocation m: leadTargets){
                        int nDist = m.distanceSquaredTo(me);
                        if(nDist < dist){
                            target = m;
                            dist = nDist;
                        }
                    }
                }
            }else{
                //search through gold targets for closest goal
                for(MapLocation m: goldTargets){
                    int nDist = m.distanceSquaredTo(me);
                    if(nDist < dist){
                        target = m;
                        dist = nDist;
                    }
                }
            }




            //Wander towards goal!
            Direction dir = efficientPath(rc, target);
            System.out.println(dir.toString());
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
    }

    static int[] readArray(RobotController rc, int start, int end){
        int[] array = new int[64];
        for (int i = start; i <= end; i++) {
            try {
                array[i] = rc.readSharedArray(i);
            } catch (GameActionException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    static void WriteToOpenSpace(RobotController rc, int[] readArray, int startIndex, int endIndex, int writeValue){
        boolean foundSpace = false;
        int spaceFound = -1;
        boolean alreadyInArray = false;

        for (int i = startIndex; i <= endIndex; i++){
            if(readArray[i] == writeValue){
                alreadyInArray = true;
                break;
            }
            if (readArray[i] == 0){
                if (!foundSpace) {
                    foundSpace = true;
                    spaceFound = i;
                }
            }
        }
        try {
            if(!alreadyInArray && foundSpace){
                rc.writeSharedArray(spaceFound, writeValue);
            }
        } catch (GameActionException e) {
            e.printStackTrace();
        }
    }

    static void WriteLocationToOpenSpace(RobotController rc, int[] readArray, int startIndex, int endIndex, MapLocation locus, int distanceSquared){
        boolean foundSpace = false;
        int spaceFound = -1;
        boolean alreadyInArray = false;

        for (int i = startIndex; i <= endIndex; i++){
            MapLocation place = ImportLocationData(readArray[i]);
            if(place != null){
                if(place.distanceSquaredTo(locus) <= distanceSquared){
                    alreadyInArray = true;
                    break;
                }
            }

            if (readArray[i] == 0){
                if (!foundSpace) {
                    foundSpace = true;
                    spaceFound = i;
                }
            }
        }
        try {
            if(!alreadyInArray && foundSpace){
                rc.writeSharedArray(spaceFound, ExportLocationData(locus));
            }
        } catch (GameActionException e) {
            e.printStackTrace();
        }
    }

    static void DeleteEntry(RobotController rc, int[] readArray, int startIndex, int endIndex, int readValue){
        for (int i = startIndex; i <= endIndex; i++){
            try {
                if(readArray[i] == readValue){
                    rc.writeSharedArray(i, 0);
                }
            } catch (GameActionException e) {
                e.printStackTrace();
            }
        }
    }

    static void DeleteLocation(RobotController rc, int[] readArray, int startIndex, int endIndex, MapLocation locus, int distanceSquared){
        for (int i = startIndex; i <= endIndex; i++){
            try {
                MapLocation place = ImportLocationData(readArray[i]);
                if(place != null){
                    if(place.distanceSquaredTo(locus) <= distanceSquared){
                        rc.writeSharedArray(i, 0);
                    }
                }
            } catch (GameActionException e) {
                e.printStackTrace();
            }
        }
    }

    static int getFirstValue(int[] readArray, int start, int end){
        for (int i = start; i <= end; i++){
            if(readArray[i] != 0){
                return readArray[i];
            }
        }
        return 0;
    }

    static MapLocation getClosestLocation(int[] readArray, int start, int end, MapLocation myLocus){
        int bestDist = Integer.MAX_VALUE;
        MapLocation best = null;

        for (int i = start; i <= end; i++){
            if(readArray[i] != 0){
                MapLocation locat = ImportLocationData(readArray[i]);
                if(locat != null){
                    int dist = locat.distanceSquaredTo(myLocus);
                    if (dist < bestDist){
                        best = locat;
                        bestDist = dist;
                    }
                }
            }
        }
        return best;
    }


    static MapLocation ImportLocationData(int arrayValue){
        if(arrayValue < 16384){
            if(arrayValue != 0){ System.out.println("Error: " + arrayValue + " is not a valid location");}
            return null;
        }
        String binaryValue = Integer.toBinaryString(arrayValue);
        String[] parts = backSplit(binaryValue, 6);
        return new MapLocation(Integer.parseInt(parts[parts.length - 2],2), Integer.parseInt(parts[parts.length - 1],2));
    }

    static String[] backSplit(String str, int n){
        String[] parts = new String[str.length()/n + 1];
        int index = parts.length - 1;
        for (int i = str.length(); i >= 0; i-=n){
            if(i - n >= 0){
                parts[index] = str.substring(i - n, i);
            }else{
                parts[index] = str.substring(0, i);
                break;
            }
            index--;
        }
        return parts;
    }


    static int ExportLocationData(MapLocation location){
        DecimalFormat df = new DecimalFormat("000000");
        String x = df.format(Integer.parseInt(Integer.toBinaryString(location.x)));
        String y = df.format(Integer.parseInt(Integer.toBinaryString(location.y)));
        String[] bin = {"0100", x, y};
        return Integer.parseInt(String.join("", bin),2);
    }

    /**
     * Run a single turn for a Soldier.
     * This code is wrapped inside the infinite loop in run(), so it is called once per turn.
     */
    static void runSoldier(RobotController rc) throws GameActionException {
        // Try to attack someone
        int radius = rc.getType().actionRadiusSquared;
        Team opponent = rc.getTeam().opponent();
        RobotInfo[] enemies = rc.senseNearbyRobots(radius, opponent);
        if (enemies.length > 0) {
            MapLocation toAttack = enemies[0].location;
            if (rc.canAttack(toAttack)) {
                rc.attack(toAttack);
            }
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }
}
