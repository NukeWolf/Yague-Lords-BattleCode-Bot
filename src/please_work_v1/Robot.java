package please_work_v1;

import battlecode.common.*;
import java.util.Random;

public class Robot {
    RobotController rc;


    /**
     * A random number generator.
     * We will use this RNG to make some random moves. The Random class is provided by the java.util.Random
     * import at the top of this file. Here, we *seed* the RNG with a constant number (6147); this makes sure
     * we get the same sequence of numbers every time this code is run. This is very useful for debugging!
     */
    final Random rng = new Random(6147);

    /** Array containing all the possible movement directions. */
    final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
    };

    final int MapWidth;
    final int MapHeight;
    GridLocation currentGridLocation;



    public int turnCount = 0;


    public Robot(RobotController robotController) throws GameActionException{
        rc = robotController;

        MapWidth = rc.getMapWidth();
        MapHeight = rc.getMapHeight();
        currentGridLocation = new GridLocation(rc);

    }
    public void run() throws GameActionException{
        turnCount += 1;  // We have now been alive for one more turn!
    }


}
