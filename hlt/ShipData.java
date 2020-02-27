package hlt;

/**
 * toMine = | assigned block for mining, shipState == MINING
            | null, shipState == RETURNING_CARGO

   toDrop = assigned drop point
 */

public class ShipData {
    public int shipId;
    public ShipState shipState;
    public Position toMine;
    public Position toDrop;

    public ShipData(int shipId, Position toDrop) {
        this.shipId = shipId;
        shipState = ShipState.RETURNING_CARGO;
        this.toDrop = toDrop;
        toMine = null;
    }

    @Override
    public String toString() {
        return "S[" + shipId + "] - " + shipState + ": M: "
                + toMine.toString() + " | D: " + toDrop.toString();
    }
}
