package faceroll;

import battlecode.common.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class Miner extends Robot {

    //final int[][] SPIRAL_ORDER = {{0, 0}, {-1, 0}, {0, -1}, {0, 1}, {1, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}, {-2, 0}, {0, -2}, {0, 2}, {2, 0}, {-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}, {-2, -2}, {-2, 2}, {2, -2}, {2, 2}, {-3, 0}, {0, -3}, {0, 3}, {3, 0}, {-3, -1}, {-3, 1}, {-1, -3}, {-1, 3}, {1, -3}, {1, 3}, {3, -1}, {3, 1}, {-3, -2}, {-3, 2}, {-2, -3}, {-2, 3}, {2, -3}, {2, 3}, {3, -2}, {3, 2}, {-4, 0}, {0, -4}, {0, 4}, {4, 0}, {-4, -1}, {-4, 1}, {-1, -4}, {-1, 4}, {1, -4}, {1, 4}, {4, -1}, {4, 1}, {-3, -3}, {-3, 3}, {3, -3}, {3, 3}, {-4, -2}, {-4, 2}, {-2, -4}, {-2, 4}, {2, -4}, {2, 4}, {4, -2}, {4, 2}};
    final int[][][] distances = {{{0, 0}}, {{-1, 0}, {0, -1}, {0, 1}, {1, 0}}, {{-1, -1}, {-1, 1}, {1, -1}, {1, 1}}, {}, {{-2, 0}, {0, -2}, {0, 2}, {2, 0}}, {{-2, -1}, {-2, 1}, {-1, -2}, {-1, 2}, {1, -2}, {1, 2}, {2, -1}, {2, 1}}, {}, {}, {{-2, -2}, {-2, 2}, {2, -2}, {2, 2}}, {{-3, 0}, {0, -3}, {0, 3}, {3, 0}}, {{-3, -1}, {-3, 1}, {-1, -3}, {-1, 3}, {1, -3}, {1, 3}, {3, -1}, {3, 1}}, {}, {}, {{-3, -2}, {-3, 2}, {-2, -3}, {-2, 3}, {2, -3}, {2, 3}, {3, -2}, {3, 2}}, {}, {}, {{-4, 0}, {0, -4}, {0, 4}, {4, 0}}, {{-4, -1}, {-4, 1}, {-1, -4}, {-1, 4}, {1, -4}, {1, 4}, {4, -1}, {4, 1}}, {{-3, -3}, {-3, 3}, {3, -3}, {3, 3}}, {}, {{-4, -2}, {-4, 2}, {-2, -4}, {-2, 4}, {2, -4}, {2, 4}, {4, -2}, {4, 2}}};


    Pathfinding pf;
    int[] tilesVisited;
    MapLocation destination;
    ArrayList<MapLocation> leadLocations = new ArrayList<>();
    MapLocation lastLeadLocation;

    public Miner(RobotController rc) throws GameActionException {
        super(rc);

        pf = new Pathfinding(rc);
        tilesVisited = new int[numCols * numRows];
        destination = getNearestLead();
    }

    public void run() throws GameActionException {
        System.out.println("Beginning run(): "+ Clock.getBytecodeNum());
        myLocation = rc.getLocation();

        tilesVisited[getTileNumber(myLocation)] = 1;

        tryMine();
        System.out.println("Ending run(): "+ Clock.getBytecodeNum());
    }

    public void tryMine() throws GameActionException {

        int distanceToDest;

        while (rc.isActionReady()) { // move is safe, so maybe just try to move always? get someone to look at this loop
            distanceToDest = myLocation.distanceSquaredTo(destination);
            if (rc.canSenseLocation(destination)) {
                if (rc.senseLead(destination) < 2) {
                    destination = getNearestLead();
                } else if (distanceToDest <= 2) {
                    rc.mineLead(destination);
                } else if (rc.isMovementReady()) {
                    pf.move(destination);
                    myLocation = rc.getLocation();
                } else {
                    break;
                }
            } else if (rc.isMovementReady()) {
                pf.move(destination);
                myLocation = rc.getLocation();
            } else {
                break;
            }
        }
        rc.setIndicatorString("I am at " + rc.getLocation() + "; My destination is " + destination); // use this to check where it's going / mining
    }

    public MapLocation getNearestLead() throws GameActionException {
        int distanceToNearest = 99999;
        MapLocation nearest = null;
        if (destination != null && !(rc.canSenseLocation(destination) && (rc.senseLead(destination) <= 1))) {
            nearest = destination;
            distanceToNearest = myLocation.distanceSquaredTo(nearest);
        }

        System.out.println("Bytecode before scan " + Clock.getBytecodeNum());
        // leadLocations.addAll(Arrays.asList(rc.senseNearbyLocationsWithLead(100))); // this is much, much better than the loop to get lead locations, costs ~ 300!

        int startScanDist = 0;
        if (distanceToNearest != 99999) {
            startScanDist = distanceToNearest;
        }

        MapLocation temp;
        int distanceToTemp;
        // costs around 3700 bytecode, consider smaller vision radius, especially w/ more advanced pathfinding / decision-making
        for (int i = startScanDist; i <= 20; i++) {
            for (int[] shift : distances[i]) {
                temp = myLocation.translate(shift[0], shift[1]);
                distanceToTemp = myLocation.distanceSquaredTo(temp);
                if (rc.canSenseLocation(temp) && distanceToTemp < distanceToNearest && rc.senseLead(temp) > 1) {
                    nearest = temp;
                    distanceToNearest = distanceToTemp;
                    System.out.println("Bytecode after scan " + Clock.getBytecodeNum());
                    return temp;
                }
            }
        }
        System.out.println("Bytecode after scan " + Clock.getBytecodeNum());  // ~380 bytecode

        // This portion costs 4700 bytecode!!
        /* Iterator<MapLocation> leadIterator = leadLocations.iterator();
        int scanRadius = 20;
        while (leadIterator.hasNext()) {
            MapLocation leadLocation = leadIterator.next();
            int leadDistance = myLocation.distanceSquaredTo(leadLocation);
            if (leadDistance < distanceToNearest) {
                // Note: Uses lead comparison instead of rc.canSenseLocation since location guaranteed to be on map
                if (leadDistance < scanRadius && rc.senseLead(leadLocation) <= 1) {
                    leadIterator.remove();
                } else {
                    nearest = leadLocation;
                    distanceToNearest = leadDistance;
                }
            }
        } */
//        System.out.println("end find nearest "+rc.getRoundNum() + " " +Clock.getBytecodeNum());

        if (nearest != null) {
            lastLeadLocation = nearest;
            return nearest;
        }
        return getNearestUnexploredTile();
    }


    public MapLocation getNearestUnexploredTile() throws GameActionException {
        int currentTile = getTileNumber(myLocation);
        int scanRadius = 20;
        for (int[][] radius : distances) {
            for (int[] shift : radius) {
                int newTile = currentTile + shift[0] + numCols * shift[1];
                if (newTile >= 0 && newTile < numRows * numCols && tilesVisited[newTile] == 0) {
                    MapLocation newTileLocation = getTileCenter(newTile);
                    if (myLocation.distanceSquaredTo(newTileLocation) >= scanRadius) {
                        //rc.setIndicatorDot(newTileLocation, 255, 0,0);
                        return newTileLocation;
                    }
                }
            }
        }
        return new MapLocation(0, 0); // explored entire map and no soup left
    }

}
