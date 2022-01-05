package examplefuncsplayer;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public enum SharedArray {
    // Start Bit, End Bit
    TESTVARIABLE (0,2);

    /**
     * Creates the AND mask to select the right bits
     */
    private final int mask;
    /**
     * Which index in the array does this access
     */
    private final int arrayIndex;

    SharedArray(int startBit, int endBit) {
        this.arrayIndex = startBit / 16;
        this.mask = 0xffff << startBit >>> startBit + 16 - endBit;
    }
    public int read(RobotController rc) throws GameActionException {
        return rc.readSharedArray(arrayIndex) & mask;
    }
}
