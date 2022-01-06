package version_1;

import battlecode.common.GameActionException;
import battlecode.common.MapLocation;
import battlecode.common.RobotController;

import java.text.DecimalFormat;

public class Calculators {

    public static int[] readArray(RobotController rc, int start, int end){
        int[] array = new int[64];
        for (int i = start; i <= end; i++) {
            try {
                array[i] = rc.readSharedArray(i);
            } catch (GameActionException e) {
                e.printStackTrace();
            }
        }
        return array;
    }

    public static void WriteToOpenSpace(RobotController rc, int[] readArray, int startIndex, int endIndex, int writeValue){
        boolean foundSpace = false;
        int spaceFound = -1;
        boolean alreadyInArray = false;

        for (int i = startIndex; i <= endIndex; i++){
            if(readArray[i] == writeValue){
                alreadyInArray = true;
                break;
            }
            if (readArray[i] == 0){
                if (!foundSpace) {
                    foundSpace = true;
                    spaceFound = i;
                }
            }
        }
        try {
            if(!alreadyInArray && foundSpace){
                rc.writeSharedArray(spaceFound, writeValue);
            }
        } catch (GameActionException e) {
            e.printStackTrace();
        }
    }

    public static void WriteLocationToOpenSpace(RobotController rc, int[] readArray, int startIndex, int endIndex, MapLocation locus, int distanceSquared){
        boolean foundSpace = false;
        int spaceFound = -1;
        boolean alreadyInArray = false;

        for (int i = startIndex; i <= endIndex; i++){
            MapLocation place = ImportLocationData(readArray[i]);
            if(place != null){
                if(place.distanceSquaredTo(locus) <= distanceSquared){
                    alreadyInArray = true;
                    break;
                }
            }

            if (readArray[i] == 0){
                if (!foundSpace) {
                    foundSpace = true;
                    spaceFound = i;
                }
            }
        }
        try {
            if(!alreadyInArray && foundSpace){
                rc.writeSharedArray(spaceFound, ExportLocationData(locus));
            }
        } catch (GameActionException e) {
            e.printStackTrace();
        }
    }

    public static void DeleteEntry(RobotController rc, int[] readArray, int startIndex, int endIndex, int readValue){
        for (int i = startIndex; i <= endIndex; i++){
            try {
                if(readArray[i] == readValue){
                    rc.writeSharedArray(i, 0);
                }
            } catch (GameActionException e) {
                e.printStackTrace();
            }
        }
    }

    public static void DeleteLocation(RobotController rc, int[] readArray, int startIndex, int endIndex, MapLocation locus, int distanceSquared){
        for (int i = startIndex; i <= endIndex; i++){
            try {
                MapLocation place = ImportLocationData(readArray[i]);
                if(place != null){
                    if(place.distanceSquaredTo(locus) <= distanceSquared){
                        rc.writeSharedArray(i, 0);
                    }
                }
            } catch (GameActionException e) {
                e.printStackTrace();
            }
        }
    }

    public static int getFirstValue(int[] readArray, int start, int end){
        for (int i = start; i <= end; i++){
            if(readArray[i] != 0){
                return readArray[i];
            }
        }
        return 0;
    }

    public static MapLocation getClosestLocation(int[] readArray, int start, int end, MapLocation myLocus){
        int bestDist = Integer.MAX_VALUE;
        MapLocation best = null;

        for (int i = start; i <= end; i++){
            if(readArray[i] != 0){
                MapLocation locat = ImportLocationData(readArray[i]);
                if(locat != null){
                    int dist = locat.distanceSquaredTo(myLocus);
                    if (dist < bestDist){
                        best = locat;
                        bestDist = dist;
                    }
                }
            }
        }
        return best;
    }


    public static MapLocation ImportLocationData(int arrayValue){
        if(arrayValue < 16384){
            if(arrayValue != 0){ System.out.println("Error: " + arrayValue + " is not a valid location");}
            return null;
        }
        String binaryValue = Integer.toBinaryString(arrayValue);
        String[] parts = backSplit(binaryValue, 6);
        return new MapLocation(Integer.parseInt(parts[parts.length - 2],2), Integer.parseInt(parts[parts.length - 1],2));
    }

    public static int ExportLocationData(MapLocation location){
        DecimalFormat df = new DecimalFormat("000000");
        String x = df.format(Integer.parseInt(Integer.toBinaryString(location.x)));
        String y = df.format(Integer.parseInt(Integer.toBinaryString(location.y)));
        String[] bin = {"0100", x, y};
        return Integer.parseInt(String.join("", bin),2);
    }

    public static String[] backSplit(String str, int n){
        String[] parts = new String[str.length()/n + 1];
        int index = parts.length - 1;
        for (int i = str.length(); i >= 0; i-=n){
            if(i - n >= 0){
                parts[index] = str.substring(i - n, i);
            }else{
                parts[index] = str.substring(0, i);
                break;
            }
            index--;
        }
        return parts;
    }

    public static MapLocation getRandomLocationFarAway(MapLocation me, int mW, int mH, int distanceReq){
        MapLocation locus = new MapLocation((int) (Math.random()*mW), (int) (Math.random()*mH));
        while (locus.distanceSquaredTo(me) <= distanceReq){
            locus = new MapLocation((int) (Math.random()*mW), (int) (Math.random()*mH));
        }
        return locus;
    }

}
