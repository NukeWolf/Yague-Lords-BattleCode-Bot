package please_work_v1;

import battlecode.common.*;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;

public class Miner extends Robot {
    //For wander navigation
    public int[][] rubbleMap; //keeps track of rubble
    public boolean[][] exploredMap; //keeps track of places on the map already been to/sensed
    public int goldMaxThreshold = 1;
    public int goldMinThreshold = 1;
    public int leadMaxThreshold = 50;
    public int leadMinThreshold = 10;
    public int myVision;
    public int myAction;

    public double exploreFactor = 0.1; //how much the robot will want to explore
    public int exploreSearchDistance = 5;
    public double distanceFactor = 0.1; //how much the robot cares about getting closer to its goal
    public double avoidSameTypeFactor = 5; //how much the robot avoid other robots of its type
    public double retreatFactor = 1.5; //how much the robot cares about moving away from known enemies (based on damage)
    public double rubbleFactor = 0.2; //how much robot cares about not stepping on rubble

    //State management
    static boolean mining = false;

    public Miner(RobotController rc) throws GameActionException{
        super(rc);
        initializeMaps(rc);
    }

    @Override
    public void run() throws GameActionException{
        super.run();
        // Try to mine on squares around us.
        MapLocation me = rc.getLocation();
        int[] arrayInfo = readArray(rc, 9,16);
        rc.setIndicatorString(Arrays.toString(arrayInfo));

        int leadSensed = 0;
        int goldSensed = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 1; dy <= 1; dy++) {
                MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                // Notice that the Miner's action cooldown is very low.
                // You can mine multiple times per turn!
                while (rc.canMineGold(mineLocation)) {
                    rc.mineGold(mineLocation);
                    mining = true;
                }
                while (rc.canMineLead(mineLocation)) {
                    rc.mineLead(mineLocation);
                    mining = true;
                }

                if (rc.canSenseLocation(mineLocation)) {
                    leadSensed += rc.senseLead(mineLocation);
                    goldSensed += rc.senseGold(mineLocation);
                }
            }
        }

        if(goldSensed > goldMaxThreshold|| leadSensed > leadMaxThreshold){
            //write to array about mylocation
            WriteLocationToOpenSpace(rc, arrayInfo,9, 16, me, 2);
        }else if(goldSensed < goldMinThreshold && leadSensed < leadMinThreshold){
            //search array for my location and remove it from the array
            DeleteLocation(rc, arrayInfo, 9, 16, me, 2);
            if (goldSensed == 0 && leadSensed == 0){
                mining = false;
            }
        }

        // Also try to move to places not yet explored.
        if(!mining) {

            int dist = Integer.MAX_VALUE;
            ArrayList<MapLocation> goldTargets = new ArrayList<MapLocation>();
            ArrayList<MapLocation> leadTargets = new ArrayList<MapLocation>();
            MapLocation target = null;

            for (int dx = - (int) Math.sqrt(myVision); dx <= (int) Math.sqrt(myVision); dx++) {
                for (int dy = - (int) Math.sqrt(myVision - Math.pow(dx,2)); dy <= (int) Math.sqrt(myVision - Math.pow(dx,2)); dy++) {
                    MapLocation mineLocation = new MapLocation(me.x + dx, me.y + dy);
                    if (rc.canSenseLocation(mineLocation)) {
                        int leadSeen = rc.senseLead(mineLocation);
                        int goldSeen = rc.senseGold(mineLocation);

                        if(goldSeen > 0){
                            goldTargets.add(mineLocation);
                        }
                        if(leadSeen > 0){
                            leadTargets.add(mineLocation);
                        }
                    }
                }
            }

            if (goldTargets.isEmpty()){
                if (leadTargets.isEmpty()){
                    //search through 8 array integers for the closest goal
                    target = getClosestLocation(arrayInfo, 9, 16, me);
                    if (target == null){
                        target = new MapLocation(rubbleMap.length / 2, rubbleMap[0].length / 2);
                    }
                }else{
                    //search through lead targets for closest goal
                    for(MapLocation m: leadTargets){
                        int nDist = m.distanceSquaredTo(me);
                        if(nDist < dist){
                            target = m;
                            dist = nDist;
                        }
                    }
                }
            }else{
                //search through gold targets for closest goal
                for(MapLocation m: goldTargets){
                    int nDist = m.distanceSquaredTo(me);
                    if(nDist < dist){
                        target = m;
                        dist = nDist;
                    }
                }
            }




            //Wander towards goal!
            Direction dir = efficientPath(rc, target);
            System.out.println(dir.toString());
            if (rc.canMove(dir)) {
                rc.move(dir);
            }
        }
    }

    public void initializeMaps(RobotController rc){
        rubbleMap = new int[rc.getMapWidth()][rc.getMapHeight()];
        exploredMap = new boolean[rc.getMapWidth()][rc.getMapHeight()];
        myVision = rc.getType().visionRadiusSquared;
        myAction = rc.getType().actionRadiusSquared;
    }

    public int[] readArray(RobotController rc, int start, int end){
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

    public void WriteToOpenSpace(RobotController rc, int[] readArray, int startIndex, int endIndex, int writeValue){
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

    public void WriteLocationToOpenSpace(RobotController rc, int[] readArray, int startIndex, int endIndex, MapLocation locus, int distanceSquared){
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

    public void DeleteEntry(RobotController rc, int[] readArray, int startIndex, int endIndex, int readValue){
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

    public void DeleteLocation(RobotController rc, int[] readArray, int startIndex, int endIndex, MapLocation locus, int distanceSquared){
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

    public int getFirstValue(int[] readArray, int start, int end){
        for (int i = start; i <= end; i++){
            if(readArray[i] != 0){
                return readArray[i];
            }
        }
        return 0;
    }

    public MapLocation getClosestLocation(int[] readArray, int start, int end, MapLocation myLocus){
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


    public MapLocation ImportLocationData(int arrayValue){
        if(arrayValue < 16384){
            if(arrayValue != 0){ System.out.println("Error: " + arrayValue + " is not a valid location");}
            return null;
        }
        String binaryValue = Integer.toBinaryString(arrayValue);
        String[] parts = backSplit(binaryValue, 6);
        return new MapLocation(Integer.parseInt(parts[parts.length - 2],2), Integer.parseInt(parts[parts.length - 1],2));
    }

    public String[] backSplit(String str, int n){
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


    public int ExportLocationData(MapLocation location){
        DecimalFormat df = new DecimalFormat("000000");
        String x = df.format(Integer.parseInt(Integer.toBinaryString(location.x)));
        String y = df.format(Integer.parseInt(Integer.toBinaryString(location.y)));
        String[] bin = {"0100", x, y};
        return Integer.parseInt(String.join("", bin),2);
    }

    public Direction efficientPath (RobotController rc, MapLocation locus) throws GameActionException{

        try {

            MapLocation m = rc.getLocation();
            RobotInfo[] robots = rc.senseNearbyRobots();

            rc.setIndicatorDot(m.add(Direction.NORTH), 255,0,0);

            for (int dx = - (int) Math.sqrt(myVision); dx <= (int) Math.sqrt(myVision); dx++){
                for (int dy = - (int) Math.sqrt(myVision - Math.pow(dx, 2)); dy <= (int) Math.sqrt(myVision - Math.pow(dx, 2)); dy++){
                    int newX = m.x + dx;
                    int newY = m.y + dy;
                    if (newX >= 0 && newX < rubbleMap.length && newY >= 0 && newY < rubbleMap[newX].length){
                        rubbleMap[newX][newY] = rc.senseRubble(new MapLocation(m.x + dx, m.y + dy));
                        exploredMap[newX][newY] = true;
                    }
                }
            }

            Direction best = null;
            double bestScore = Integer.MIN_VALUE;

            for (int i = 0; i < 8; i++){

                if(!rc.canMove(directions[i])){
                    continue;
                }
                //function = unexplored*k - distance - rubble - enemies
                double score = 0;
                MapLocation locat = m.add(directions[i]);

                score += (m.distanceSquaredTo(locus) - locat.distanceSquaredTo(locus)) * distanceFactor;

                for (int dx = -exploreSearchDistance; dx <= exploreSearchDistance; dx++){
                    for (int dy = -exploreSearchDistance; dy <= exploreSearchDistance; dy++){
                        int newX = m.x + dx;
                        int newY = m.y + dy;
                        if (newX >= 0 && newX < rubbleMap.length && newY >= 0 && newY < rubbleMap[newX].length){
                            if(!exploredMap[newX][newY]){
                                score += exploreFactor;
                            }
                        }
                    }
                }

                score -= rubbleMap[locat.x][locat.y] * rubbleFactor;

                for (RobotInfo robot: robots) {
                    if (robot.team != rc.getTeam() && locat.distanceSquaredTo(robot.location) <= robot.type.actionRadiusSquared) {
                        score -= robot.getType().damage * retreatFactor;
                    }

                    if (robot.team == rc.getTeam() && robot.type == rc.getType() && locat.distanceSquaredTo(robot.location) <= robot.type.visionRadiusSquared){
                        score -= avoidSameTypeFactor;
                    }
                }

                if (score >= bestScore){
                    bestScore = score;
                    best = directions[i];
                }

            }
            if(best != null){
                return best;
            }else{
                return Direction.CENTER;
            }



        } catch (GameActionException e){
            System.out.println("Direction error " + rc.getLocation());
            e.printStackTrace();
            return directions[rng.nextInt(directions.length)];
        }catch ( Exception e){
            e.printStackTrace();
            return directions[rng.nextInt(directions.length)];
        }
    }
}
