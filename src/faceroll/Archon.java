package faceroll;

import battlecode.common.*;

public class Archon extends Robot {

    int minerCount;
    int soldierCount;

    public Archon(RobotController rc) throws GameActionException {
        super(rc);
    }

    @Override
    public void run() throws GameActionException {
        if (rc.getRoundNum() == 500) rc.resign();
        if (minerCount < 25) {
            for (Direction dir : Direction.allDirections()) {
                if (rc.canBuildRobot(RobotType.MINER, dir)) {
                    rc.buildRobot(RobotType.MINER, dir);
                }
            }
        } else if (soldierCount < minerCount) {
            for (Direction dir : Direction.allDirections()) { // do it towards the enemy archon???
                if (rc.canBuildRobot(RobotType.MINER, dir)) {
                    rc.buildRobot(RobotType.MINER, dir);
                }
            }
        }
    }
}
