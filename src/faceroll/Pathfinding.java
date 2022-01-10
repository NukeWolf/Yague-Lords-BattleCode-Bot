package faceroll;

import battlecode.common.*;

import java.util.HashSet;

public class Pathfinding{

    RobotController rc;
    MapLocation destination;
    double avgMoveCost = 10;

    BugNav bugNav = new BugNav();

    public Pathfinding(RobotController rc) {
        this.rc = rc;
    }

    static final Direction[] directions = {
            Direction.NORTH,
            Direction.NORTHEAST,
            Direction.EAST,
            Direction.SOUTHEAST,
            Direction.SOUTH,
            Direction.SOUTHWEST,
            Direction.WEST,
            Direction.NORTHWEST,
            Direction.CENTER
    };

    boolean canMove(Direction dir) throws GameActionException {
        if (!rc.canMove(dir)) return false;
        // if (impassable[dir.ordinal()]) return false; //TODO: add in impassability
        return true;
    }

    public void move(MapLocation loc) throws GameActionException{
        if (!rc.isMovementReady()) return;
        destination = loc;

        rc.setIndicatorLine(rc.getLocation(), destination, 255, 0, 0);

        if (!bugNav.move()) greedyPath();
        bugNav.move();
    }


    double getEstimation (MapLocation loc){ // roughly, the number of move actions this will take
        try {
            if (loc.distanceSquaredTo(destination) == 0) return 0;
            int d = distance(destination, loc);
            int p = rc.senseRubble(loc);
            return 1+p/10.0 + (d - 1)*avgMoveCost;
        } catch (Throwable e){
            e.printStackTrace();
        }
        return 1e9; // in case of fail, say that it will take 10^9 move actions
    }

    static boolean strictlyCloser(MapLocation newLoc, MapLocation oldLoc, MapLocation target){
        int dOld = distance(target, oldLoc), dNew = distance(target, newLoc);
        if (dOld < dNew) return false;
        if (dNew < dOld) return true;
        return target.distanceSquaredTo(newLoc) < target.distanceSquaredTo(oldLoc);
    }

    static int distance(MapLocation A, MapLocation B){
        return Math.max(Math.abs(A.x - B.x), Math.abs(A.y - B.y)); // we get max bc of diagonal moves
    }

    final double eps = 1e-5; // a small number used for "close enough" i.e. if abs(a-b) < eps, they're close enough

    void greedyPath() throws  GameActionException{ // takes the fastest move in the direction of the target
        MapLocation myLoc = rc.getLocation();
        Direction bestDir = null;
        double bestEstimation = 0;
        double firstStep = 1.0 + rc.senseRubble(myLoc)/10.0;
        int tilesCounted = 0;
        double bestEstimationDist = 0;
        int totalRubble = 0;
        for (Direction dir : directions) {
            MapLocation newLoc = myLoc.add(dir);
            if (!rc.onTheMap(newLoc)) continue;

            //pass
            totalRubble += rc.senseRubble(newLoc);
            ++tilesCounted;


            // if (!canMove(dir)) continue;    This is for when you want to mark unmovable tiles (including dangerous ones)
            if (!strictlyCloser(newLoc, myLoc, destination)) continue;

            int newDist = newLoc.distanceSquaredTo(destination);

            double estimation = firstStep + getEstimation(newLoc);
            // the if statement checks the following: no direction yet, faster direction, diagonal direction (usually faster)
            if (bestDir == null || estimation < bestEstimation - eps || (Math.abs(estimation - bestEstimation) <= 2*eps && newDist < bestEstimationDist)) {
                bestEstimation = estimation;
                bestDir = dir;
                bestEstimationDist = newDist;
            }
        }

        if (tilesCounted != 0) {
            avgMoveCost = 1 + totalRubble / (tilesCounted * 10.0) ;
        }
        if (bestDir != null) rc.move(bestDir);
    }

    // TODO: Implement the dijkstra's in the github
    //  costs around 4500 bytecode, can be optimized to 4000; starter optimizations below
    // https://discord.com/channels/386965718572466197/401058232346345473/928994128983908352
    class BugNav{

        BugNav(){}

        final int INF = 1000000;
        final int MAX_MAP_SIZE = GameConstants.MAP_MAX_HEIGHT;

        boolean rotateRight = true; //if I should rotate right or left
        MapLocation lastObstacleFound = null; //latest obstacle I've found in my way
        int minDistToEnemy = INF; //minimum distance I've been to the enemy while going around an obstacle
        MapLocation prevDestination = null; //previous target
        HashSet<Integer> visited = new HashSet<>();

        boolean move() throws GameActionException{
            //different target? ==> previous data does not help!
            if (prevDestination == null || destination.distanceSquaredTo(prevDestination) > 0) resetPathfinding();

            //If I'm at a minimum distance to the target, I'm free!
            MapLocation myLoc = rc.getLocation();
            int d = myLoc.distanceSquaredTo(destination);
            if (d <= minDistToEnemy) resetPathfinding();  // too close to enemies

            int code = rc.hashCode();

            if (visited.contains(code)) resetPathfinding(); // if we've been here before
            visited.add(code);

            //Update data
            prevDestination = destination;
            minDistToEnemy = Math.min(d, minDistToEnemy);

            //If there's an obstacle I try to go around it [until I'm free] instead of going to the target directly
            Direction dir = myLoc.directionTo(destination);
            if (lastObstacleFound != null) dir = myLoc.directionTo(lastObstacleFound);
            if (canMove(dir)){
                resetPathfinding();
            }

            //I rotate clockwise or counterclockwise (depends on 'rotateRight'). If I try to go out of the map I change the orientation
            //Note that we have to try at most 16 times since we can switch orientation in the middle of the loop. (It can be done more efficiently)
            for (int i = 8; i-- > 0;) {
                if (canMove(dir)) {
                    rc.move(dir);
                    return true;
                }
                MapLocation newLoc = myLoc.add(dir);
                if (!rc.onTheMap(newLoc)) rotateRight = !rotateRight;
                    //If I could not go in that direction and it was not outside of the map, then this is the latest obstacle found
                else lastObstacleFound = myLoc.add(dir);
                if (rotateRight) dir = dir.rotateRight();
                else dir = dir.rotateLeft();
            }

            if (canMove(dir)) rc.move(dir);
            return true;
        }

        //clear some of the previous data
        void resetPathfinding(){
            lastObstacleFound = null;
            minDistToEnemy = INF;
            visited.clear();
        }
        // 17 bits that tell you in order: x, y, where the obstacle is, and which way to turn around the obstacle, could just use hashcode?
    }
}
