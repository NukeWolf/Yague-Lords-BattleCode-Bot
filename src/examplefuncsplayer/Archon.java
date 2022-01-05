package examplefuncsplayer;

import battlecode.common.*;


public class Archon extends Robot {
    int soldierRatio = 1;
    int minerRatio = 2;
    RobotType currentUnit = RobotType.MINER;
    int currentUnitBuilt = 0;

    int bytesUsed = 0;


    public Archon(RobotController rc) throws GameActionException{
        super(rc);
    }
    @Override
    public void run() throws GameActionException{
        super.run();



        SharedArray.TESTVARIABLE.read(rc);
        SharedArray.TESTVARIABLE.read(rc);
        //rc.writeSharedArray(0,16384);

        switch (currentUnit){
            case MINER:
                if (currentUnitBuilt >= minerRatio) {
                    currentUnit = RobotType.SOLDIER;
                    currentUnitBuilt = 0;
                }
                break;
            case SOLDIER:
                if (currentUnitBuilt >= soldierRatio) {
                    currentUnit = RobotType.MINER;
                    currentUnitBuilt = 0;
                }
                break;
        }

        for(Direction dir : directions){
            if (rc.canBuildRobot(currentUnit, dir)) {
                rc.buildRobot(currentUnit, dir);
                currentUnitBuilt += 1;
            }
            break;
        }
        System.out.println(Clock.getBytecodeNum());
        bytesUsed += Clock.getBytecodeNum();
        rc.setIndicatorString(String.valueOf(bytesUsed/turnCount));
    }

}
