package please_work_v1;

import battlecode.common.*;

public class GridLocation {
    final int gridSize = 4;
    public int x;
    public int y;

    public int mapWidth;
    public int mapHeight;

    public int gridWidth;
    public int gridHeight;

    public MapLocation mapLocation;

    public int index;
    public GridLocation(RobotController rc) throws GameActionException {
        mapLocation = rc.getLocation();
        mapHeight =rc.getMapHeight();
        mapWidth = rc.getMapWidth();

        gridWidth = mapWidth / 4;
        gridHeight = mapHeight / 4;

        x = mapLocation.x / gridWidth;
        y = mapLocation.y / gridHeight;

    }
    public int[] getClosestLocations() throws GameActionException{
        int[] output = new int[16];
        int count = 0;
        int lowerX = 0 - x;
        int upperX = gridSize - 1 - x;
        int lowerY = 0 - y;
        int upperY = gridSize - 1 - y;

        for(int manhattan = 0; count < 16; manhattan += 1){
            for (int dx = 0; dx <= manhattan; dx++) {
                int dy = manhattan - dx;
                if (dx <= upperX) {
                    if (dy <= upperY) {
                        output[count] = (y + dy) * gridSize + x + dx;
                        count++;
                    }
                    if (-dy >= lowerY && dy != 0) {
                        output[count] = (y - dy) * gridSize + x + dx;
                        count++;
                    }
                }
                if (-dx >= lowerX && dx != 0) {
                    if (dy <= upperY) {
                        output[count] = (y + dy) * gridSize + x - dx;
                        count++;
                    }
                    if (-dy >= lowerY && dy != 0) {
                        output[count] = (y - dy) * gridSize + x - dx;
                        count++;
                    }
                }
            }
        }
        return output;

    }
}
