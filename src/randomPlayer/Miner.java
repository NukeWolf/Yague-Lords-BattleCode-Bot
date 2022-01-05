package randomPlayer;

import battlecode.common.*;

public class Miner extends Robot {
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

    public Miner(RobotController rc) throws GameActionException{
        super(rc);

    }

    @Override
    public void run() throws GameActionException{
        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                }
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                }
            }
        }

        // Also try to move randomly.
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rc.canMove(dir)) {
            rc.move(dir);
        }
    }


}
