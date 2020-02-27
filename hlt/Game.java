package hlt;

import java.util.ArrayList;
import java.util.Collection;

public class Game {
    public int turnNumber;
    public final PlayerId myId;
    public final ArrayList<Player> players = new ArrayList<>();
    public final Player me;
    public final GameMap gameMap;

    public Game() {
        Constants.populateConstants(Input.readLine());

        final Input input = Input.readInput();
        final int numPlayers = input.getInt();
        myId = new PlayerId(input.getInt());

        Log.open(myId.id);

        for (int i = 0; i < numPlayers; ++i) {
            players.add(Player._generate());
        }
        me = players.get(myId.id);
        gameMap = GameMap._generate();
    }

    public void ready(final String name) {
        System.out.println(name);
    }

    public void updateFrame() {
        turnNumber = Input.readInput().getInt();
        Log.log("=============== TURN " + turnNumber + " ================");

        for (int i = 0; i < players.size(); ++i) {
            final Input input = Input.readInput();

            final PlayerId currentPlayerId = new PlayerId(input.getInt());
            final int numShips = input.getInt();
            final int numDropoffs = input.getInt();
            final int halite = input.getInt();

            players.get(currentPlayerId.id)._update(numShips, numDropoffs, halite);
        }

        gameMap._update();

        for (final Player player : players) {
            for (final Ship ship : player.ships.values()) {
                gameMap.at(ship).markUnsafe(ship);
            }

            gameMap.at(player.shipyard).structure = player.shipyard;

            for (final Dropoff dropoff : player.dropoffs.values()) {
                gameMap.at(dropoff).structure = dropoff;
            }
        }
    }

    public void endTurn(final Collection<Command> commands) {
        for (final Command command : commands) {
            System.out.print(command.command);
            System.out.print(' ');
        }
        System.out.println();
    }

    /*
        TODO[1] [] : Tweak the magic numbers and make them constants for ease of use
            - surprisingly, for me at least, tweaking the numbers to obtain an
            optimum in the formula (that I currently don't know) has a HUGE
            impact over the efficiency of the bot.
     */
    public boolean willMove(Ship ship) {
        if (gameMap.at(ship).halite >= 110 && ship.halite <= Constants.MAX_HALITE - 20) {
            return false;
        }
        if (gameMap.at(ship).halite / 10 > ship.halite) {
            return false;
        }

        return true;
    }

    public boolean isSafeToSpawn(ArrayList<Ship> movingShips, SDB sdb) {

        ArrayList<Position> unsafePositions = me.shipyard.position.getSurroundingCardinals();
        for (Ship ship : movingShips) {
            if (sdb.get(ship.id.id).shipState == ShipState.RETURNING_CARGO
                && unsafePositions.contains(ship.position)) {
                return false;
            }

            if (ship.position.equals(me.shipyard.position)) {
                return false;
            }
        }

        return true;
    }
}
