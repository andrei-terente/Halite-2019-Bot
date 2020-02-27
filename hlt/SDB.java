package hlt;

import java.util.ArrayList;
import java.util.HashMap;


/**
 * Ship DataBase
 * - structure used to store additional information
 * about all existing ships on the board, such as state etc.
 *
 * - the specific data stored of one ship is contained
 * in ShipData instances
 *
 * - the database is updated every round
 */
public class SDB {
    HashMap<Integer, ShipData> database;

    public SDB() {
        database = new HashMap<>();
    }

    /**
     * Iterates over existing ships in the game instance:
     *  - if the database contains info on a ship that no
     *  longer exists in the game instance, it is removed
     *  from storage
     *
     *  - if a ship was just spawned it is added to the
     *  database with the correct parameters
     *
     * @param game , game instance
     */
    public void update(Game game) {
        ArrayList<Integer> existingIDs = new ArrayList<>();
        for (Ship ship : game.me.ships.values()) {
            existingIDs.add(ship.id.id);
            if (database.get(ship.id.id) == null) {
                database.put(ship.id.id,
                        new ShipData(ship.id.id, game.me.shipyard.position));
            }
        }

        ArrayList<Integer> toBeRemoved = new ArrayList<>();

        for (Integer id : database.keySet()) {
            if (!existingIDs.contains(id)) {
                toBeRemoved.add(id);
            }
        }

        for (Integer id : toBeRemoved) {
            database.remove(id);
        }
    }

    public ShipData get(int id) {
        return database.get(id);
    }

    public void setStateOf(Ship ship, ShipState shipState) {
        database.get(ship.id.id).shipState = shipState;
    }

    public void setMinePosOf(Ship ship, Position pos) {
        database.get(ship.id.id).toMine = pos;
    }

    public void setDropPosOf(Ship ship, Position pos) {
        database.get(ship.id.id).toDrop = pos;
    }
}
