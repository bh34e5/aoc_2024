import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        RMap.Cell start = m.findCell(m.startR, m.startC, RMap.EAST);
        System.out.println(start.getValue());
    }

    private static void partTwo(String contents) {
        RMap m = new RMap(contents);
        RMap.Cell start = m.findCell(m.startR, m.startC, RMap.EAST);

        Set<RMap.Cell> pathCells = new HashSet<>();
        LinkedList<RMap.Cell> toProcessCells = new LinkedList<>();
        toProcessCells.add(start);
        while (!toProcessCells.isEmpty()) {
            RMap.Cell first = toProcessCells.pop();
            if (pathCells.add(first)) {
                // haven't processed it, process the path cells for this one
                toProcessCells.removeAll(first.getValueCells()); // process only once
                toProcessCells.addAll(first.getValueCells());
            }
        }

        Map<Integer, RMap.Cell> places = pathCells.stream()
                .collect(Collectors.toMap(c -> c.row * m.width + c.col, c -> c, (c1, c2) -> c1));
        System.out.println(places.size());
    }

}

class RMap {
    public static final int NORTH = 0;
    public static final int EAST = 1;
    public static final int SOUTH = 2;
    public static final int WEST = 3;

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

        int startR = -1;
        int startC = -1;
        int endR = -1;
        int endC = -1;

        List<Cell> cells = new LinkedList<>();

        for (int r = 0; r < height; ++r) {
            String row = rows[r];
            for (int c = 0; c < width; ++c) {
                char cellChar = row.charAt(c);
                if (cellChar != '#') {
                    for (int d = 0; d < 4; ++d) {
                        cells.add(new Cell(r, c, d));
                    }
                }

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
        if (startR == -1 || startC == -1) {
            throw new IllegalStateException("No starts");
        }
        if (endR == -1 || endC == -1) {
            throw new IllegalStateException("No ends");
        }

        this.cells = cells;
        this.height = height;
        this.width = width;
        this.startR = startR;
        this.startC = startC;
        this.endR = endR;
        this.endC = endC;

        this.addConnections(rows, cells);
        Set<Cell> toProcess = cells.stream().filter(c -> c.row == this.endR && c.col == this.endC)
                .flatMap(cell -> cell.setValue(0).stream())
                .collect(Collectors.toSet());

        while (!toProcess.isEmpty()) {
            toProcess = toProcess.stream().map(Cell::checkValues).flatMap(List::stream).collect(Collectors.toSet());
        }
    }

    public Cell findCell(int r, int c, int d) {
        return findCell(this.cells, r, c, d);
    }

    private static Cell findCell(List<Cell> cells, int r, int c, int d) {
        return cells.stream().filter(other -> other.row == r && other.col == c && other.dir == d).findFirst()
                .orElseThrow();
    }

    private void addConnections(String[] rows, List<Cell> cells) {
        for (Cell cell : cells) {
            int r = cell.row;
            int c = cell.col;

            for (int d = 0; d < 4; ++d) {
                if (d != cell.dir) {
                    cell.addConnection(findCell(cells, r, c, d)); // get to all the directions in the same cell
                }
            }

            // connect to the cell "behind" this one
            switch (cell.dir) {
                case NORTH:
                    addConnectionIfValid(rows, cells, cell, cell.dir, r + 1, c);
                    break;
                case EAST:
                    addConnectionIfValid(rows, cells, cell, cell.dir, r, c - 1);
                    break;
                case SOUTH:
                    addConnectionIfValid(rows, cells, cell, cell.dir, r - 1, c);
                    break;
                case WEST:
                    addConnectionIfValid(rows, cells, cell, cell.dir, r, c + 1);
                    break;
                default:
                    throw new IllegalStateException("Invalid cell direction");
            }
        }
    }

    private void addConnectionIfValid(String[] rows, List<Cell> cells, Cell cell, int dir, int nr, int nc) {
        char ch = rows[nr].charAt(nc);
        if (ch != '#') {
            cell.addConnection(findCell(cells, nr, nc, dir));
        }
    }

    class Cell {
        public final int row;
        public final int col;
        public final int dir;
        private final Set<Cell> inConnections;
        private final Set<Cell> outConnections;
        private final Set<Cell> valueCells;
        private int value;

        public Cell(int row, int col, int dir) {
            this.row = row;
            this.col = col;
            this.dir = dir;
            this.inConnections = new HashSet<>();
            this.outConnections = new HashSet<>();
            this.valueCells = new HashSet<>(); // not powered by any cell
            this.value = -1; // unintialized
        }

        public void addConnection(Cell cell) {
            this.outConnections.add(cell);
            cell.inConnections.add(this);
        }

        public List<Cell> setValue(int value) {
            this.valueCells.clear();
            this.value = value;
            return new LinkedList<>(this.outConnections);
        }

        public Set<Cell> getValueCells() {
            if (this.value == -1) {
                throw new IllegalStateException("Unintialized cell");
            }
            return this.valueCells;
        }

        public int getValue() {
            if (this.value == -1) {
                throw new IllegalStateException("Unintialized cell");
            }
            return this.value;
        }

        private List<Cell> checkValues() {
            Set<Cell> minCells = new HashSet<>();
            int minValue = Integer.MAX_VALUE;

            for (Cell o : this.inConnections) {
                int oValue = o.value;
                if (oValue == -1) {
                    continue;
                }

                int testVal = oValue + (isRotation(o) ? 1000 * rotationFrom(o) : 1);
                if (testVal < minValue) {
                    minCells.clear();
                    minCells.add(o);
                    minValue = testVal;
                } else if (testVal == minValue) {
                    minCells.add(o);
                }
            }

            if (!minCells.isEmpty() && (this.value == -1 || minValue < this.value)) {
                this.valueCells.clear();
                this.valueCells.addAll(minCells);
                this.value = minValue;
                return new LinkedList<>(this.outConnections);
            }
            return Collections.emptyList();
        }

        public boolean isRotation(Cell other) {
            return this.row == other.row && this.col == other.col && this.dir != other.dir;
        }

        public int rotationFrom(Cell other) {
            if (!isRotation(other)) {
                throw new IllegalArgumentException("Cell is not a rotation away");
            }
            return ((this.dir - other.dir) % 2) == 0 ? 2 : 1;
        }
    }
}
