import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Work {
    public static void main(String[] args) throws IOException {
        String filename = args[0];
        String contents = Files.readString(Path.of(filename));

        int part = 1;
        if (args.length > 1) {
            try {
                part = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignore) {
            }
        }

        switch (part) {
            case 1:
                partOne(contents);
                break;
            case 2:
                partTwo(contents);
                break;
            default:
                throw new IllegalArgumentException("Bad part");
        }
    }

    private static void partOne(String contents) {
        RMap m = new RMap(contents);

        Map<Integer, Integer> counts = new HashMap<>();
        // check horizontal walls
        for (int r = 0; r < m.height - 2; ++r) {
            for (int c = 0; c < m.width; ++c) {
                if (m.isEmptyCell(r + 0, c) && m.isEmptyCell(r + 2, c) && !m.isEmptyCell(r + 1, c)) {
                    RMap.Cell cT = m.findCell(r + 0, c);
                    RMap.Cell cB = m.findCell(r + 2, c);

                    int diff = Math.abs(cT.getValue() - cB.getValue()) - 2; // minus two for the travel time
                    counts.put(diff, counts.getOrDefault(diff, 0) + 1);
                }
            }
        }
        // check vertical walls
        for (int r = 0; r < m.height; ++r) {
            for (int c = 0; c < m.width - 2; ++c) {
                if (m.isEmptyCell(r, c + 0) && m.isEmptyCell(r, c + 2) && !m.isEmptyCell(r, c + 1)) {
                    RMap.Cell cL = m.findCell(r, c + 0);
                    RMap.Cell cR = m.findCell(r, c + 2);

                    int diff = Math.abs(cL.getValue() - cR.getValue()) - 2; // minus two for the travel time
                    counts.put(diff, counts.getOrDefault(diff, 0) + 1);
                }
            }
        }

        int total = 0;
        for (Integer key : counts.keySet()) {
            if (key >= 100) {
                total += counts.get(key);
            }
        }
        System.out.println("Total = " + total);
    }

    private static Stream<Map.Entry<Integer, Integer>> stepsAway(Map.Entry<Integer, Integer> center, int step) {
        Map.Entry<Integer, Integer> start = Map.entry(center.getKey() - step, center.getValue());

        UnaryOperator<Map.Entry<Integer, Integer>> getNext = cur -> {
            if (cur.getKey() < center.getKey() && cur.getValue() >= center.getValue()) {
                // quadrant one
                return Map.entry(cur.getKey() + 1, cur.getValue() + 1);
            } else if (cur.getKey() >= center.getKey() && cur.getValue() > center.getValue()) {
                // quadrant two
                return Map.entry(cur.getKey() + 1, cur.getValue() - 1);
            } else if (cur.getKey() > center.getKey() && cur.getValue() <= center.getValue()) {
                // quadrant three
                return Map.entry(cur.getKey() - 1, cur.getValue() - 1);
            } else if (cur.getKey() <= center.getKey() && cur.getValue() < center.getValue()) {
                // quadrant four
                return Map.entry(cur.getKey() - 1, cur.getValue() + 1);
            } else {
                throw new IllegalStateException("Bad state");
            }
        };
        return Stream.concat(
                Stream.of(start),
                Stream.iterate(
                        getNext.apply(start),
                        cur -> {
                            return !((cur.getKey() - start.getKey() == 0) && (cur.getValue() - start.getValue() == 0));
                        },
                        getNext));
    }

    private static void partTwo(String contents) {
        RMap m = new RMap(contents);

        long total = IntStream.range(0, m.height).boxed().parallel().mapToLong(
                r -> {
                    System.out.println(r);
                    return IntStream.range(0, m.width).boxed().parallel()
                            .filter(c -> m.isEmptyCell(r, c))
                            .mapToLong(c -> {
                                int thisValue = m.findCell(r, c).getValue();
                                Map.Entry<Integer, Integer> center = Map.entry(r, c);

                                return IntStream.rangeClosed(2, 20).boxed().parallel()
                                        .mapToLong(
                                                dist -> stepsAway(center, dist)
                                                        .filter(m::isEmptyCell)
                                                        .map(m::findCell)
                                                        .map(RMap.Cell::getValue)
                                                        .filter(v -> (v < thisValue))
                                                        .map(v -> thisValue - (v + dist))
                                                        .filter(v -> v >= 100)
                                                        .count())
                                        .sum();
                            })
                            .sum();
                })
                .sum();
        System.out.println(total);
    }
}

class RMap {
    private final String[] rows;
    private final List<Cell> cells;
    public final int height;
    public final int width;
    public final int startR;
    public final int startC;
    public final int endR;
    public final int endC;

    public RMap(String contents) {
        String[] rows = contents.strip().split("\n");
        int height = rows.length;
        int width = rows[0].length();

        if (!Arrays.stream(rows).allMatch(r -> r.length() == width)) {
            throw new AssertionError("Non-rectangular map");
        }

        List<Cell> cells = new LinkedList<>();
        int startR = -1;
        int startC = -1;
        int endR = -1;
        int endC = -1;

        for (int r = 0; r < height; ++r) {
            String row = rows[r];
            for (int c = 0; c < width; ++c) {
                char cellChar = row.charAt(c);
                if (cellChar != '#') {
                    cells.add(new Cell(r, c));

                    if (cellChar == 'S') {
                        if (startR != -1 && startC != -1) {
                            throw new IllegalStateException("Multiple starts");
                        }
                        startR = r;
                        startC = c;
                    } else if (cellChar == 'E') {
                        if (endR != -1 && endC != -1) {
                            throw new IllegalStateException("Multiple ends");
                        }
                        endR = r;
                        endC = c;
                    }
                }
            }
        }
        if (startR == -1 || startC == -1) {
            throw new IllegalStateException("No starts");
        }
        if (endR == -1 || endC == -1) {
            throw new IllegalStateException("No ends");
        }

        this.rows = rows;
        this.cells = cells;
        this.height = height;
        this.width = width;
        this.startR = startR;
        this.startC = startC;
        this.endR = endR;
        this.endC = endC;

        this.addConnections();

        Set<Cell> toProcess = findCell(this.endR, this.endC).setValue(0);
        while (!toProcess.isEmpty()) {
            toProcess = toProcess.stream()
                    .map(Cell::checkValues)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
        }
    }

    public Cell findCell(Map.Entry<Integer, Integer> position) {
        return findCell(position.getKey(), position.getValue());
    }

    public Cell findCell(int r, int c) {
        return findCells(r, c).findFirst().orElseThrow();
    }

    private Stream<Cell> findCells(int r, int c) {
        return this.cells.stream().filter(other -> other.row == r && other.col == c);
    }

    private void addConnections() {
        for (Cell cell : this.cells) {
            int r = cell.row;
            int c = cell.col;

            addConnectionIfValid(cell, r - 1, c);
            addConnectionIfValid(cell, r, c + 1);
            addConnectionIfValid(cell, r + 1, c);
            addConnectionIfValid(cell, r, c - 1);
        }
    }

    private void addConnectionIfValid(Cell cell, int nr, int nc) {
        if (isEmptyCell(nr, nc)) {
            cell.addConnection(findCell(nr, nc));
        }
    }

    public boolean isValid(int r, int c) {
        return ((0 <= r && r < this.height) && (0 <= c && c < this.width));
    }

    public boolean isEmptyCell(Map.Entry<Integer, Integer> position) {
        return isEmptyCell(position.getKey(), position.getValue());
    }

    public boolean isEmptyCell(int r, int c) {
        return isValid(r, c) && charAt(r, c) != '#';
    }

    private char charAt(int r, int c) {
        if (r < 0 || r >= this.height) {
            throw new IndexOutOfBoundsException("Invalid row");
        }
        if (c < 0 || c >= this.width) {
            throw new IndexOutOfBoundsException("Invalid column");
        }
        return this.rows[r].charAt(c);
    }

    class Cell {
        public final int row;
        public final int col;
        private final Set<Cell> inConnections;
        private final Set<Cell> outConnections;
        private final Set<Cell> valueCells;
        private int value;

        public Cell(int row, int col) {
            this.row = row;
            this.col = col;
            this.inConnections = new HashSet<>();
            this.outConnections = new HashSet<>();
            this.valueCells = new HashSet<>(); // not powered by any cell
            this.value = -1; // unintialized
        }

        public void addConnection(Cell cell) {
            this.outConnections.add(cell);
            cell.inConnections.add(this);
        }

        public Set<Cell> setValue(int value) {
            if (value < 0) {
                throw new IllegalArgumentException("Invalid cell value");
            }
            this.valueCells.clear();
            this.value = value;
            return this.outConnections;
        }

        public Set<Cell> getValueCells() {
            if (this.value == -1) {
                throw new IllegalStateException("Uninitialized cell");
            }
            return this.valueCells;
        }

        public int getValue() {
            if (this.value == -1) {
                throw new IllegalStateException("Uninitialized cell");
            }
            return this.value;
        }

        public boolean isValid() {
            return this.value != -1;
        }

        private Set<Cell> checkValues() {
            if (this.value != -1 && this.valueCells.isEmpty()) {
                return Collections.emptySet();
            }

            Set<Cell> minCells = new HashSet<>();
            int minValue = -1;

            for (Cell o : this.inConnections) {
                int oValue = o.value;
                if (oValue == -1) {
                    continue;
                }

                int testVal = oValue + 1;
                if (minValue == -1 || testVal < minValue) {
                    minCells.clear();
                    minCells.add(o);
                    minValue = testVal;
                } else if (testVal == minValue) {
                    minCells.add(o);
                }
            }

            if ((this.value == -1 && minValue != -1) || minValue < this.value) {
                this.valueCells.clear();
                this.valueCells.addAll(minCells);
                this.value = minValue;
                return this.outConnections;
            }
            return Collections.emptySet();
        }
    }
}
