package randomPlayer;

import battlecode.common.*;


public class Archon extends Robot {
    int soldierRatio = 1;
    int builderRatio = 2;


    public Archon(RobotController rc) throws GameActionException{
        super(rc);
    }
    @Override
    public void run() throws GameActionException{
        Direction dir = directions[rng.nextInt(directions.length)];
        if (rng.nextBoolean()) {
            // Let's try to build a miner.
            //rc.setIndicatorString("Trying to build a miner");
            if (rc.canBuildRobot(RobotType.MINER, dir)) {
                rc.buildRobot(RobotType.MINER, dir);
            }
        } else {
            // Let's try to build a soldier.
            //rc.setIndicatorString("Trying to build a soldier");
            if (rc.canBuildRobot(RobotType.SOLDIER, dir)) {
                rc.buildRobot(RobotType.SOLDIER, dir);
            }
        }
    }
}
