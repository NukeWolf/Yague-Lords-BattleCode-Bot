package examplefuncsplayer;

import battlecode.common.GameActionException;
import battlecode.common.RobotController;

public enum SharedArray {
    // Start Bit, End Bit
    TESTVARIABLE (0,0,1),
    TESTVAR2 (0,8,12)
    ;
    /**
     * Creates the AND mask to select a certain amount of bits to the right. For Reading
     */
    private final int readMask;
    /**
     * How many binary digits to shift to the right to remove the least significant digits. For Reading
     */
    private final int rightShift;
    /**
     * Which index in the array does this access
     */
    private final int arrayIndex;


    /**
     * How many binary digits to shift to the left to make digits more significant. For writing
     */
    private final int leftShift;
    /**
     * Creates the AND mask to unset certain binary digits. For writing.
     */
    private final int writeMask;

    SharedArray(int arrayIndex,int startBit, int endBit) {

        this.arrayIndex = arrayIndex;
        //For Reading
        this.readMask = 0xffff >>> 16 - (endBit - startBit);
        this.rightShift = 16-endBit;

        //For writing
        this.leftShift = 16-endBit;
        // Creates a string of 1s of length of the memory address and shifts it into place
        this.writeMask = ((1 << (endBit-startBit)) -1) << leftShift;

    }
    public int read(RobotController rc) throws GameActionException {
        //First shifts to the right the digits not needed and then selects the amount of binary digits needed to the right.
        return rc.readSharedArray(arrayIndex) >>> rightShift & readMask;
    }
    public void write(int value,RobotController rc) throws GameActionException{
        //Apply a set mask and then an unset mask.
        int shifted = value<<leftShift;
        rc.writeSharedArray(arrayIndex,(rc.readSharedArray(arrayIndex) | shifted) & ~(writeMask ^ shifted));
    }
}
