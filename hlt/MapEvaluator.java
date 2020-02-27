package hlt;

import javafx.geometry.Pos;

import java.util.*;

public class MapEvaluator {
    static private MapEvaluator ourInstance;

    static public MapEvaluator getInstance(Game game) {
        if (ourInstance == null) {
            ourInstance = new MapEvaluator(game);
        }

        return ourInstance;
    }

    static private Game game;
    static private int sqrLen = 6;

    private MapEvaluator(Game _game) {
        game = _game;
    }

    public void updateState(Game _game) {
        game = _game;
    }

    public ArrayList<Integer> getBestSqr() {
        int max_halite_sum = 0;
        GameMap map = game.gameMap;

        int left_row = 0;
        int left_col = 0;
        int right_row = 0;
        int right_col = 0;

        for (int i = 0; i < map.height - sqrLen; ++i) {
            for (int j = 0; j < map.width - sqrLen; ++j) {
                int crt_halite_sum = 0;

                for (int r = i; r < i + sqrLen; ++r) {
                    for (int k = j; k < j + sqrLen; ++k) {
                        crt_halite_sum += map.cells[r][k].halite;
                    }
                }

                if (crt_halite_sum >= max_halite_sum) {
                    max_halite_sum = crt_halite_sum;
                    left_row = i;
                    left_col = j;
                    right_row = i + sqrLen - 1;
                    right_col = j + sqrLen - 1;
                }
            }
        }

        ArrayList<Integer> ret = new ArrayList<>();
        ret.add(left_row);
        ret.add(left_col);
        ret.add(right_row);
        ret.add(right_col);

        return ret;
    }

    static class PositionComparator
            implements Comparator<Position>
    {
        @Override
        public int compare(Position o1, Position o2) {
            return game.gameMap.at(o2).halite - game.gameMap.at(o1).halite;
        }
    }

    static class SqrSumComparator
            implements Comparator<Map.Entry<Position, Integer>>
    {
        @Override
        public int compare(Map.Entry<Position, Integer> o1, Map.Entry<Position, Integer> o2) {
            return o2.getValue() - o1.getValue();
        }
    }

    public ArrayList<Position> getSquareSortedCenters() {
        GameMap map = game.gameMap;
        HashMap<Position, Integer> allCenters = new HashMap<>();

        for (int i = 0; i < map.height - sqrLen; i += sqrLen) {
            for (int j = 0; j < map.width - sqrLen; j += sqrLen) {
                int crt_halite_sum = 0;

                for (int r = i; r < i + sqrLen; ++r) {
                    for (int k = j; k < j + sqrLen; ++k) {
                        crt_halite_sum += map.cells[r][k].halite;
                    }
                }

                allCenters.put(new Position(j + sqrLen/2, i + sqrLen/2), crt_halite_sum);
            }
        }

        List<Map.Entry<Position, Integer>> entries = new LinkedList<>(allCenters.entrySet());

        Collections.sort(entries, new SqrSumComparator());

        ArrayList<Position> sortedCenters = new ArrayList<>();

        for (Map.Entry<Position, Integer> entry : entries) {
            sortedCenters.add(entry.getKey());
        }


        return sortedCenters;
    }
}

