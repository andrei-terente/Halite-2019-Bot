package hlt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class TrafficManager {
    public static final int STUCK_THRESHOLD = 1;

    HashMap<Integer, Integer> roundsStill =  new HashMap<>();
    HashMap<Integer, Position> previousPositions = new HashMap<>();
    SDB database;
    DropoffManager dropoffManager;

    public void attachSDB_DOM(SDB sdb, DropoffManager _dropoffManager) {
        database = sdb;
        dropoffManager = _dropoffManager;
    }

    public void update(Game game) {
        ArrayList<Integer> existingShips = new ArrayList<>();


        for (Ship ship : game.me.ships.values()) {
            existingShips.add(ship.id.id);

            // ADDING NEWLY SPAWNED SHIPS
            if (previousPositions.get(ship.id.id) == null) {
                previousPositions.put(ship.id.id, ship.position);
                roundsStill.put(ship.id.id, 0);
                continue;
            }

            // UPDATING TRAFFIC STATUS

            // Verifying current position and previous position for each ship
            // If the two match, we do an additional check to decide if the
            // ship is staying still to gather resources, or if it was jammed
            // The values of roundsStill are updated accordingly
            if (previousPositions.get(ship.id.id).equals(ship.position)) {
                if (
                        (ship.position.equals(database.get(ship.id.id).toMine)
                            && database.get(ship.id.id).shipState.equals(ShipState.MINING))
                        || ((game.gameMap.at(ship).halite >= 55)
                            && (Constants.MAX_HALITE - ship.halite >= 55))
                        || (dropoffManager.isChosenOne(ship))
                ) {
                    roundsStill.put(ship.id.id, 0);
                } else {
                    roundsStill.put(ship.id.id, roundsStill.get(ship.id.id) + 1);
                }
            } else {
                roundsStill.put(ship.id.id, 0);
                previousPositions.put(ship.id.id, ship.position);
            }
        }


        // REMOVING SHIPS THAT WERE DESTROYED IN THE PREVIOUS ROUND
        ArrayList<Integer> toBeRemoved = new ArrayList<>();
        for (Integer id : roundsStill.keySet()) {
            if (!existingShips.contains(id)) {
                toBeRemoved.add(id);
            }
        }

        for (Integer id : toBeRemoved) {
            roundsStill.remove(id);
            previousPositions.remove(id);
        }

    }


    public boolean isStuck(Ship ship) {
        return (roundsStill.get(ship.id.id) > STUCK_THRESHOLD);
    }

    public Direction getAdditionalOffset(Direction d) {
        switch (d) {
            case NORTH:
                return Direction.EAST;
            case SOUTH:
                return Direction.WEST;
            case EAST:
                return Direction.SOUTH;
            case WEST:
                return Direction.NORTH;
            default:
                return Direction.NORTH;
        }
    }

    public Direction decideMove(Ship ship, Game game) {
        ArrayList<Direction> cardinals = Direction.ALL_CARDINALS;
        ArrayList<Direction> possibleMoves = new ArrayList<>();
        ArrayList<Ship> waitingShips = new ArrayList<>();

        for (Direction d : cardinals) {
            if (!game.gameMap.at(ship.position.directionalOffset(d)).isOccupied()
                && !(game.gameMap.at(ship.position.directionalOffset(d)).position.equals(game.me.shipyard.position))) {
                possibleMoves.add(d);
                continue;
            }

            waitingShips.add(game.gameMap.at(ship.position.directionalOffset(d)).ship);

            /*
                AO O AO
                 O O O   , where O = offset, AO = additional offset
                AO O AO

                checks additional offsets for ships to minimise collisions;
             */

//            if (game.gameMap.at(ship.position.directionalOffset(d)
//                    .directionalOffset(getAdditionalOffset(d))).isOccupied()) {
//                Ship aoShip = game.gameMap.at(ship.position.directionalOffset(d)
//                        .directionalOffset(getAdditionalOffset(d))).ship;
//                if (isStuck(aoShip)) {
//                    waitingShips.add(aoShip);
//                }
//            }
        }



        if (possibleMoves.isEmpty()) {
            return Direction.STILL;
        }


        Position destination = (database.get(ship.id.id).shipState.equals(ShipState.MINING))
                ? (database.get(ship.id.id).toMine)
                : (database.get(ship.id.id).toDrop);

        // Saving moves that will get the ship closer to the destination
        ArrayList<Direction> movesToDestination = new ArrayList<>();
        int d_x = destination.x - ship.position.x;
        int d_y = destination.y - ship.position.y;

        if (d_y < 0) {
            movesToDestination.add(Direction.EAST);
        } else if (d_y > 0) {
            movesToDestination.add(Direction.WEST);
        }

        if (d_x < 0) {
            movesToDestination.add(Direction.SOUTH);
        } else if (d_x > 0) {
            movesToDestination.add(Direction.NORTH);
        }

        // Saving moves that will better help clearing the jam
        ArrayList<Direction> movesToUnjam = new ArrayList<>();
        int yaxis_traffic = 0;
        int xaxis_traffic = 0;

        for (Direction direction : cardinals) {
            if (direction.equals(Direction.NORTH) || direction.equals(Direction.SOUTH)
                && game.gameMap.at(ship.position.directionalOffset(direction)).isOccupied())
            {
                yaxis_traffic++;
                continue;
            }

            if (direction.equals(Direction.EAST) || direction.equals(Direction.WEST)
                    && game.gameMap.at(ship.position.directionalOffset(direction)).isOccupied())
            {
                xaxis_traffic++;
            }
        }

        if (xaxis_traffic > yaxis_traffic) {
            movesToUnjam.add(Direction.SOUTH);
            movesToUnjam.add(Direction.NORTH);
        } else if (yaxis_traffic > xaxis_traffic) {
            movesToUnjam.add(Direction.EAST);
            movesToUnjam.add(Direction.WEST);
        } else {
            movesToUnjam.addAll(Direction.ALL_CARDINALS);
        }


        // Saving optimal moves (that both get the ship closer to the
        // destination AND help to better clear the jam)
        int intersections = 0;

        ArrayList<Direction> optimalMoves = new ArrayList<>();
        for (Direction direction : movesToUnjam) {
            if (movesToDestination.contains(direction)) {
                optimalMoves.add(direction);
                intersections++;
            }
        }

        // If there are no such moves, we consider the optimum
        // to be the moves that clear the traffic better
        if (intersections == 0) {
            optimalMoves.clear();
            optimalMoves = movesToUnjam;
        }

        // TODO[1] [DONE] : Test whether shuffling the list is beneficial
        /*
         Shuffling determines the choice made out of the possible moves. (the bot always picks possibleMoves[0])
         In some cases, shuffling is beneficial, as in END_HALITE_SHUFFLING > END_HALITE_PICKING_FIRST.
         This means there is a factor in deciding which move out of the possible ones is optimal, therefore:
        */
        // TODO[1.1] [] : Find a rule that maximizes total halite based on the optimal choice of possible moves
        // Collections.shuffle(possibleMoves);

        // TODO[2] [DONE] : Test whether reducing the roundsStill value to 0 or decreasing it is better
        /*
            The question is not relevant since it was determined that the optimal
            STUCK_THRESHOLD value is 1. Therefore, decrementing the value or setting it to 0
            is literally the same action;
         */

        for (Ship wS : waitingShips) {
            if (wS != null) {
                int newValue = 0;
                roundsStill.put(wS.id.id, newValue);
            }
        }

        // Collections.shuffle(optimalMoves);

        // Check if any of the optimal moves are possible
        for (Direction direction : possibleMoves) {
            if (optimalMoves.contains(direction)) {
                return direction;
            }
        }

        // Collections.shuffle(possibleMoves);

        // If the optimal spots are all occupied, simply move
        // in the first spot available so the other ships
        // can go around the ship
        return possibleMoves.get(0);
    }
}
