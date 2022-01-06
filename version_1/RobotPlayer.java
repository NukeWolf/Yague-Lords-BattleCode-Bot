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
    static int myVision;
    static int myAction;

    public static double exploreFactor = 0.1; //how much the robot will want to explore
    public static int exploreSearchDistance = 5;
    public static double distanceFactor = 0.1; //how much the robot cares about getting closer to its goal
    public static double avoidSameTypeFactor = 5; //how much the robot avoid other robots of its type
    public static double retreatFactor = 4; //how much the robot cares about moving away from known enemies (based on damage)
    public static double rubbleFactor = 0.2; //how much robot cares about not stepping on rubble

    public static MapLocation randomTarget;
    public static int targetRefreshRate = 100;

    //State management
    static boolean mining = false;
    static String currentTarget = null;

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
                    if (robot.team != rc.getTeam() && locat.distanceSquaredTo(robot.location) <= robot.type.actionRadiusSquared+3) {
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
        //System.out.println(Arrays.toString(rubbleMap[0]));

        // You can also use indicators to save debug notes in replays.
        //rc.setIndicatorString("Hello world!");

        while (true) {
            // This code runs during the entire lifespan of the robot, which is why it is in an infinite
            // loop. If we ever leave this loop and return from run(), the robot dies! At the end of the
            // loop, we call Clock.yield(), signifying that we've done everything we want to do.

            MapLocation me = rc.getLocation();

            if(turnCount % targetRefreshRate == 0 || me.distanceSquaredTo(randomTarget) < 20){
                randomTarget = Calculators.getRandomLocationFarAway(me, rubbleMap.length, rubbleMap[0].length, 50);
            }

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
        int[] arrayInfo = Calculators.readArray(rc, 9,16);
        int leadSensed = 0;
        int goldSensed = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
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

        if(goldSensed > 0|| leadSensed > 50){
            //write to array about mylocation
            Calculators.WriteLocationToOpenSpace(rc, arrayInfo,9, 16, me, 2);
        }else if(goldSensed <= 0 && leadSensed < 10){
            //search array for my location and remove it from the array
            Calculators.DeleteLocation(rc, arrayInfo, 9, 16, me, 2);
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
                    target = Calculators.getClosestLocation(arrayInfo, 9, 16, me);
                    if (target == null){
                        target = randomTarget;
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

            currentTarget = target.x + " " + target.y;

            //Wander towards goal!
            Direction dir = efficientPath(rc, target);
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }

        rc.setIndicatorString(currentTarget);
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
