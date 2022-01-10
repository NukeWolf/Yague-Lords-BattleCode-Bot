package faceroll;

import battlecode.common.*;

public abstract class Robot {

    RobotController rc;

    // Updated every turn
    MapLocation myLocation;

    // Dividing the map into a grid of 3x3 'tiles'
    final int MAP_WIDTH;
    final int MAP_HEIGHT;

    // These are the tile parameters
    final int tileWidth = 3;
    final int tileHeight = 3;
    final int numCols;
    final int numRows;

    public Robot(RobotController rc) throws GameActionException{
        this.rc = rc;

        myLocation = rc.getLocation();
        MAP_WIDTH = rc.getMapWidth();
        MAP_HEIGHT = rc.getMapHeight();
        numCols = (MAP_WIDTH + tileWidth - 1) / tileWidth;
        numRows = (MAP_HEIGHT + tileHeight - 1) / tileHeight;
    }

    public abstract void run() throws GameActionException;

    /*
     * Grid Functions:
     *      The grid is a grid of tileWidth x tileHeight tiles with tile number 0
     *      being the bottom-left tile.
     */

    public int getTileNumber(MapLocation loc) throws GameActionException{
        int tileX = loc.x / tileWidth;
        int tileY = loc.y / tileHeight;
        return tileX + numCols * tileY;
    }

    public MapLocation getTileCenter(int tnum) throws GameActionException {
        int col = tnum % numCols;
        int row = tnum / numCols;
        int centerX = Math.min(tileWidth * col + tileWidth/2, MAP_WIDTH - 1); // Min is for tiles 1 or 2 wide bc they're on the edge of the map
        int centerY = Math.min(tileHeight * row + tileHeight/2, MAP_HEIGHT - 1);
        return new MapLocation(centerX, centerY);

    }
}
