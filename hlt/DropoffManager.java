package hlt;

import java.util.ArrayList;

public class DropoffManager {
    /*
        BIG TODO[1] [] : Implement an actual formula to choosing when and where to make a dropoff
     */

    public static final int HALITE_THRESHOLD = 2 * Constants.DROPOFF_COST;
    public static final int MAX_DROPOFFS = 2;
    public static final int MIN_SHIPS = 2;

    private static Game game;
    private static MapEvaluator eval;
    private static SDB sdb = null;
    private static Logger logger = Logger.getInstance();

    private static ArrayList<Position> possibleDropoffPositions;

    private Position nextDropoffPosition = null;

    private static int chosenOne = -1; // unassigned
    private static boolean startedOperation = false;
    private static boolean isInPosition = false;



    public DropoffManager(Game _game, SDB _sdb) {
        game = _game;
        sdb = _sdb;
        eval = MapEvaluator.getInstance(_game);

        possibleDropoffPositions = eval.getSquareSortedCenters();
    }

    public void updateState(Game _game) {
        game = _game;
    }

    public Position checkOperation() {
        if (startedOperation) {
            return null;
        }

        if (
                // game.me.halite < HALITE_THRESHOLD
                howMany() >= MAX_DROPOFFS
                || game.me.ships.size() < MIN_SHIPS
        )
        {
            startedOperation = false;
            return null;
        }

        try {
            logger.enter(howMany());
            logger.enter(game.me.ships.size());
        } catch (Exception e) {
            // do nothing
        }

        startedOperation = true;
        chooseNextDropoffPos();

        return nextDropoffPosition;
    }

    public boolean operationStarted() {
        return startedOperation;
    }

    public boolean isInPosition() {
        return isInPosition;
    }

    public void chooseNextDropoffPos() {
//        ArrayList<Integer> boundaries = eval.getBestSqr();
//
//        int _y = (boundaries.get(0) + boundaries.get(2)) / 2;
//        int _x = (boundaries.get(1) + boundaries.get(3)) / 2;
//
//        nextDropoffPosition = new Position(_x, _y);

        nextDropoffPosition = possibleDropoffPositions.get(0);

        // possibleDropoffPositions = eval.getSquareSortedCenters();
        possibleDropoffPositions.remove(0);

        if (possibleDropoffPositions.get(0).equals(nextDropoffPosition)) {
            possibleDropoffPositions.remove(0);
        }


        if (howMany() > 0) {
            ArrayList<Position> removable = new ArrayList<>();
            for (Dropoff dropoff : game.me.dropoffs.values()) {
                possibleDropoffPositions.remove(dropoff.position);

                for (Position nextdoff : possibleDropoffPositions) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(dropoff.position);
                    stringBuilder.append(" ");
                    stringBuilder.append(nextdoff);
                    stringBuilder.append(" : ");
                    stringBuilder.append(game.gameMap.calculateDistance(nextdoff, dropoff.position));

                    try {
                        logger.enter(stringBuilder);
                    } catch (Exception e) {
                        // nothing to do
                    }
                    if (game.gameMap.calculateDistance(nextdoff, dropoff.position) < 20) {
                        removable.add(nextdoff);
                    }
                }
            }
            possibleDropoffPositions.removeAll(removable);
        }

        try {
            logger.enter("DISTANCE: " + game.gameMap.calculateDistance(nextDropoffPosition, game.me.shipyard.position));
        } catch (Exception e) {
            // nothing to do
        }
    }

    public Ship chooseShipToTransform(ArrayList<Ship> ships) {
        if (!operationStarted()) {
            return null;
        }

        int lowest_distance = Integer.MAX_VALUE;
        Ship chosen_ship = null;

        if (nextDropoffPosition == null) {
            return chosen_ship;
        }

        for (Ship ship : ships) {
            int distance_to_point =
                    game.gameMap.calculateDistance(ship.position, nextDropoffPosition);

            if (distance_to_point < lowest_distance) {
                lowest_distance = distance_to_point;
                chosen_ship = ship;
            }
        }

        if (chosen_ship != null) {
            try {
                logger.enter(chosen_ship);
            } catch (Exception e) {
                // nothing to do
            }
            chosenOne = chosen_ship.id.id;
            startedOperation = true;

            sdb.setStateOf(chosen_ship, ShipState.RETURNING_CARGO);
            sdb.setDropPosOf(chosen_ship, nextDropoffPosition);
        }
        return chosen_ship;
    }

    public Command nextMove(Ship ship) {
        if (ship.position.equals(nextDropoffPosition)) {
            isInPosition = true;
            if (game.me.halite >= Constants.DROPOFF_COST) {
                isInPosition = false;
                nextDropoffPosition = null;
                chosenOne = -1;
                startedOperation = false;
                return ship.makeDropoff();
            } else {
                return ship.stayStill();
            }
        } else {
            return ship.move(game.gameMap.naiveNavigate(ship, nextDropoffPosition));
        }
    }

    public boolean isChosenOne(Ship ship) {
        return (ship.id.id == chosenOne);
    }

    public boolean shipChosen() {
        return chosenOne >= 0;
    }

    public Position closestDropPoint(Ship ship) {
        if (howMany() == 0) {
            return game.me.shipyard.position;
        }

        Position closest_drop_point = game.me.shipyard.position;
        int distance_to_shipyard =
                game.gameMap.calculateDistance(ship.position, game.me.shipyard.position);

        int min_distance = distance_to_shipyard;

        for (Dropoff dOff : game.me.dropoffs.values()) {
            int distance_to_crt =
                    game.gameMap.calculateDistance(ship.position, dOff.position);

            if (distance_to_crt < distance_to_shipyard && distance_to_crt < min_distance) {
                min_distance = distance_to_crt;
                closest_drop_point = dOff.position;
            }
        }

        return closest_drop_point;
    }

    public int howMany() {
        if (game.me.dropoffs.isEmpty()) {
            return 0;
        } else {
            return game.me.dropoffs.size();
        }
    }
}
