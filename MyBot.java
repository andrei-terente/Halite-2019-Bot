// This Java API uses camelCase instead of the snake_case as documented in the API docs.
//     Otherwise the names of methods are consistent.

import hlt.*;
import javafx.geometry.Pos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/** Tweaks that can be made based on experimenting with this stable bot
 *  TODO-MAIN-BOT:
 *  - update the bot to use this version of TrafficManager:
 *   > STUCK_THRESHOLD <- 1, optimal number as it minimises turns wasted
 *
 *   > Logic to determine if the ship is stuck or simply mining resources
 *     !! additional-task: determine if ship is mining resources ON THE
 *     PATH to its mining position. Currently a ship is considered to be
 *     stationary due to gathering only when it is stationed on the
 *     assigned mining cell. There are instances where a ship would stay
 *     still for more than 1 round to gather resources from mine-worthy
 *     cells on its path. The TrafficManager will intervene and move the ship,
 *     wasting turns in the process as the ship will turn back to the square
 *     the turn after it is "unstuck".
 *     !! solution?:
 *      > implement condition to determine said situation
 *          OR
 *      > (?) implementing a PathManager that will choose the min-cost-paths
 *      for ships will also solve this minor problem because ships won't
 *      need to stop in their path to mine. (?) - implementing a PathManager
 *      might rid the bot of the need to "manage traffic" in the way TrafficManager
 *      does it, as the desired paths of any to any point will be always known,
 *      so the bot will know if following a specific path will get the ship
 *      stuck at a specific point
 *
 *   > MyBot logic to first determine which ships will stay still, then issue commands
 *   for the moving ships
 *
 *   > MyBot logic to make the ship that was "unstuck" by TrafficManager
 *     stay still the following round to give way to still "stuck" ships
 *     !! HUGE IMPROVEMENT, as it drastically reduces the instances where
 *     ships would "dance" left and right until the specific combination
 *     of their positions would make one of them move to its desired position,
 *     therefore clearing the traffic jam
 *
 *   > Logic to update the traffic state of the ships stationed in all of
 *     the directional offsets of the ships that is being "unstuck". This
 *     determines the mentioned ships to seek to move to their desired position,
 *     rather than trying to move out of way to unjam an already clear path
 */

/**
 * BIG BIG TODO: Implement a better naive navigate
 */


public class MyBot {
    static private SDB sdb = new SDB();
    static private TrafficManager trafficManager = new TrafficManager();
    static private MapEvaluator mapEval;
    static private DropoffManager dropoffManager;

    static private Logger logger = Logger.getInstance();


    // PRIMITIVE GREEDY BLOCK START
    static boolean[][] isAssigned;

    static void initAssignMatrix(Game game) {
        isAssigned = new boolean[game.gameMap.height][game.gameMap.width];
    }

    static ArrayList<Integer> getDialBoundaries(Game game) {
        int midX = game.gameMap.height / 2;
        int midY = game.gameMap.width / 2;

        int sX = game.me.shipyard.position.y;
        int sY = game.me.shipyard.position.x;

        ArrayList<Integer> boundaries = new ArrayList<>();

        // I
        if (sX <= midX && sY <= midY) {
            boundaries.add(0);
            boundaries.add(midX);
            boundaries.add(0);
            boundaries.add(midY);
        }

        // II
        if (sX <= midX && sY > midY) {
            boundaries.add(0);
            boundaries.add(midX);
            boundaries.add(midY);
            boundaries.add(game.gameMap.width);
        }

        // III
        if (sX > midX && sY <= midY) {
            boundaries.add(midX);
            boundaries.add(game.gameMap.height);
            boundaries.add(0);
            boundaries.add(midY);
        }

        // IV
        if (sX > midX && sY > midY) {
            boundaries.add(midX);
            boundaries.add(game.gameMap.height);
            boundaries.add(midY);
            boundaries.add(game.gameMap.width);
        }
        return boundaries;
    }

    static ArrayList<Integer> getSquareBoundaries(Game game, int offset) {
        Position drop = game.me.shipyard.position;
        int height = game.gameMap.height;
        int width = game.gameMap.width;

        ArrayList<Integer> boundaries = new ArrayList<>();

        boundaries.add(Math.max(0, drop.y - offset));
        boundaries.add(Math.min(height, drop.y + offset));
        boundaries.add(Math.max(0, drop.x - offset));
        boundaries.add(Math.min(width, drop.x + offset));

        return boundaries;
    }

    static Position dialGreedyChoice(Ship ship, Game game, ArrayList<Integer> boundaries) {
        int i_start = boundaries.get(0);
        int i_end = boundaries.get(1);
        int j_start = boundaries.get(2);
        int j_end = boundaries.get(3);

        // OVERRIDE SIDE-PLAY TESTING
        //j_start = game.gameMap.width / 2;


        int max_halite = Integer.MIN_VALUE;
        Position max_halite_pos = new Position(0, 0);

        if (game.gameMap.height < 40) {

            for (int i = i_start; i < i_end; ++i) {
                for (int j = j_start; j < j_end; ++j) {
                    int current_halite = game.gameMap.cells[i][j].halite;
                    if (current_halite > max_halite && !isAssigned[i][j]) {
                        max_halite = current_halite;
                        max_halite_pos = game.gameMap.cells[i][j].position;
                    }
                }
            }

            return max_halite_pos;

        }


        int best_distance = Integer.MAX_VALUE;

        for (int i = i_start; i < i_end; ++i) {
            for (int j = j_start; j < j_end; ++j) {
                MapCell current_cell = game.gameMap.cells[i][j];
                Position dropPos = sdb.get(ship.id.id).toDrop;

                // int max_to_drop = game.gameMap.calculateDistance(max_halite_pos, dropPos);

                int current_halite = current_cell.halite;
                // int cell_to_drop_cost = game.gameMap.calculateDistance(current_cell.position, dropPos);
                int ship_to_cell_cost = game.gameMap.calculateDistance(ship.position, current_cell.position);

                int m_s = current_halite - (ship_to_cell_cost); // + cell_to_drop_cost);
                int m_d = max_halite - (best_distance); // + max_to_drop);

                if (m_s > m_d && !isAssigned[i][j]) {
                    max_halite = current_halite;
                    max_halite_pos = current_cell.position;
                    best_distance = ship_to_cell_cost;
                }
            }
        }

        return max_halite_pos;
    }

    static int getTotalHaliteInDial(Game game, ArrayList<Integer> boundaries) {
        int i_start = boundaries.get(0);
        int i_end = boundaries.get(1);
        int j_start = boundaries.get(2);
        int j_end = boundaries.get(3);

        int total_halite = 0;

        for (int i = i_start; i < i_end; ++i) {
            for (int j = j_start; j < j_end; ++j) {
                total_halite += game.gameMap.cells[i][j].halite;
            }
        }

        return total_halite;
    }

    static Direction decideMove(Game game, Ship ship, ArrayList<Integer> boundaries) {
        ShipData shipData = sdb.get(ship.id.id);

        if (shipData.shipState == ShipState.RETURNING_CARGO) {

            if (ship.position.equals(shipData.toDrop)) {
                // Update ship state
                Position greedyChoice = dialGreedyChoice(ship, game, boundaries);
                sdb.setStateOf(ship, ShipState.MINING);
                sdb.setMinePosOf(ship, greedyChoice);
                // Update assignation map
                isAssigned[greedyChoice.y][greedyChoice.x] = true;
                // Return next move towards the mining point
                return game.gameMap.naiveNavigate(ship, greedyChoice);
            } else {
                return game.gameMap.naiveNavigate(ship, shipData.toDrop);
            }

        }

        if (shipData.shipState == ShipState.MINING) {
            if (ship.halite >= Constants.MAX_HALITE - 100
                    || (game.gameMap.at(shipData.toMine).halite <= 100))
            {
                isAssigned[shipData.toMine.y][shipData.toMine.x] = false;
                sdb.setStateOf(ship, ShipState.RETURNING_CARGO);
                sdb.setMinePosOf(ship, null);
                sdb.setDropPosOf(ship, dropoffManager.closestDropPoint(ship));

                return game.gameMap.naiveNavigate(ship, shipData.toDrop);
            } else {
                return game.gameMap.naiveNavigate(ship, shipData.toMine);
            }
        }


        return Direction.STILL;
    }
    // PRIMITIVE GREEDY BLOCK END

    static ArrayList<Ship> reorderShipList(ArrayList<Ship> ships, Game game) {
        ArrayList<Ship> reorderedList = new ArrayList<>();

        for (int i = 0; i < game.gameMap.height; ++i) {
            for (int j = 0; j < game.gameMap.width; ++j) {
                Ship crt_ship = game.gameMap.cells[i][j].ship;
                if (crt_ship != null && ships.contains(crt_ship)) {
                    reorderedList.add(crt_ship);
                }
            }
        }

        return reorderedList;
    }

    public static void main(final String[] args) throws IOException {
        final long rngSeed;
        if (args.length > 1) {
            rngSeed = Integer.parseInt(args[1]);
        } else {
            rngSeed = System.nanoTime();
        }
        final Random rng = new Random(rngSeed);

        Game game = new Game();
        dropoffManager = new DropoffManager(game, sdb);

        int offset = (game.gameMap.height);
        ArrayList<Integer> boundaries = getSquareBoundaries(game, offset);

        mapEval = MapEvaluator.getInstance(game);
        logger.enter(mapEval.getBestSqr());


        initAssignMatrix(game);

        // At this point "game" variable is populated with initial map data.
        // This is a good place to do computationally expensive start-up pre-processing.
        // As soon as you call "ready" function below, the 2 second per turn timer will start.
        game.ready("MyJavaBot");

        Log.log("Successfully created bot! My Player ID is " + game.myId + ". Bot rng seed is " + rngSeed + ".");

        // SORTED SQR CENTERS CALL
        logger.enter(mapEval.getSquareSortedCenters());

        // ATTACHING DATABASE TO TRAFFIC MANAGER
        trafficManager.attachSDB_DOM(sdb, dropoffManager);

        ArrayList<Ship> movingShips = new ArrayList<>();
        ArrayList<Integer> waitingForShipsToPass = new ArrayList<>();

        int nrShips = 0;
        int maxShips = 32; // basically uncapped

            for (;;) {
            game.updateFrame();
            final Player me = game.me;
            final GameMap gameMap = game.gameMap;

            final ArrayList<Command> commandQueue = new ArrayList<>();

            // UPDATING "OBSERVERS"
            sdb.update(game);
            trafficManager.update(game);
            mapEval.updateState(game);
            dropoffManager.updateState(game);

            movingShips.clear();

            for (final Ship ship : me.ships.values()) {

                // part of TODO[1]
                if (waitingForShipsToPass.contains(ship.id.id)) {
                    commandQueue.add(ship.stayStill());
                    waitingForShipsToPass.remove((Integer) ship.id.id);
                    continue;
                }

                if (game.willMove(ship)) {
                   movingShips.add(ship);
                } else {
                   commandQueue.add(ship.stayStill());
                }
            }
            // Collections.shuffle(movingShips);

            // TODO[3] [DONE] : Add logic for making dropoffs (just one, for now)
            Position dOff = dropoffManager.checkOperation();
            if (dropoffManager.operationStarted()) {
                if (!dropoffManager.shipChosen()) {
                    dropoffManager.chooseShipToTransform(movingShips);
                    isAssigned[dOff.y][dOff.x] = true;
                }
            }

            for (final Ship ship : movingShips) {
                if (trafficManager.isStuck(ship)) {
                    Direction direction = trafficManager.decideMove(ship, game);
                    // TODO[1] [DONE] : experiment with making the ship stay still next round to let the other ships pass
                    /*
                        CONCLUSION: Making the ship stay still to give way definitely improves
                                    the bot's efficiency;
                     */
                    waitingForShipsToPass.add(ship.id.id);
                    commandQueue.add(ship.move(gameMap.naiveNavigate(ship, ship.position.directionalOffset(direction))));
                    //commandQueue.add(ship.move(direction));
                } else if (dropoffManager.isChosenOne(ship)) {
                    commandQueue.add(dropoffManager.nextMove(ship));
                } else {
                    commandQueue.add(ship.move(decideMove(game, ship, boundaries)));
                }
            }


            // TODO[2] [] : Transform this ugly if into a boolean function shouldSpawn()
            if (
                    game.turnNumber <= 200
                    // && !dropoffManager.isInPosition()
                    && game.me.halite >= 1000
                    && game.isSafeToSpawn(movingShips, sdb)
                    && nrShips < maxShips
                    //&& !(dropoffManager.operationStarted() && dropoffManager.howMany() < 1)
                    && ((game.turnNumber % 4    ) == 0)
                        || (game.turnNumber == 1)
            ) {

                logger.enter(game.turnNumber + " " + dropoffManager.operationStarted());

                commandQueue.add(game.me.shipyard.spawn());
                nrShips++;
            }

            logger.endTurn(game);
            game.endTurn(commandQueue);
        }
    }
}
