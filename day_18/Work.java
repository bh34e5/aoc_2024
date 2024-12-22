import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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

        boolean test = !filename.equalsIgnoreCase("input.txt");
        int width = test ? 7 : 71;
        int height = test ? 7 : 71;
        int count = test ? 12 : 1024;

        switch (part) {
            case 1:
                partOne(contents, height, width, count);
                break;
            case 2:
                partTwo(contents, height, width, count);
                break;
            default:
                throw new IllegalArgumentException("Bad part");
        }
    }

    private static char[][] buildMapChars(int height, int width) {
        char[][] mapChars = new char[height][width];
        for (int r = 0; r < height; ++r) {
            for (int c = 0; c < width; ++c) {
                mapChars[r][c] = '.';
            }
        }
        return mapChars;
    }

    private static char[][] buildMapChars(String[] rows, int count, int height, int width) {
        char[][] mapChars = buildMapChars(height, width);
        for (int r = 0; r < height; ++r) {
            for (int c = 0; c < width; ++c) {
                mapChars[r][c] = '.';
            }
        }

        for (int i = 0; i < count; ++i) {
            String[] coords = rows[i].split(",");
            if (coords.length != 2) {
                throw new IllegalStateException("Illegal input");
            }
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            mapChars[y][x] = '#';
        }
        return mapChars;
    }

    private static void partOne(String contents, int height, int width, int count) {
        String[] rows = contents.split("\n");
        char[][] mapChars = buildMapChars(rows, count, height, width);

        RMap m = new RMap(mapChars, height, width);
        RMap.Cell start = m.findCell(m.startR, m.startC);
        System.out.println(start.getValue());
    }

    private static void partTwo(String contents, int height, int width, int count) {
        char[][] mapChars = buildMapChars(height, width);

        RMap m = new RMap(mapChars, height, width);

        String[] rows = contents.split("\n");
        for (String row : rows) {
            String[] coords = row.split(",");
            if (coords.length != 2) {
                throw new IllegalStateException("Illegal input");
            }
            int x = Integer.parseInt(coords[0]);
            int y = Integer.parseInt(coords[1]);
            m.removeCell(y, x);
            mapChars[y][x] = '#';

            RMap.Cell start = m.findCell(0, 0);
            if (!start.isValid()) {
                System.out.println(row);
                break;
            }
        }
    }
}

class RMap {
    private final char[][] mapChars;
    private final List<Cell> cells;
    public final int height;
    public final int width;
    public final int startR;
    public final int startC;
    public final int endR;
    public final int endC;

    public RMap(char[][] mapChars, int height, int width) {
        this.mapChars = mapChars;
        this.cells = new LinkedList<>();
        this.height = height;
        this.width = width;
        this.startR = 0;
        this.startC = 0;
        this.endR = height - 1;
        this.endC = width - 1;

        for (int r = 0; r < height; ++r) {
            for (int c = 0; c < width; ++c) {
                if (mapChars[r][c] != '#') {
                    this.cells.add(new Cell(r, c));
                }
            }
        }

        this.addConnections();

        Set<Cell> toProcess = findCell(this.endR, this.endC).setValue(0);
        while (!toProcess.isEmpty()) {
            toProcess = toProcess.stream()
                    .map(Cell::checkValues)
                    .flatMap(Set::stream)
                    .collect(Collectors.toSet());
        }
    }

    public void removeCell(int r, int c) {
        Cell cell = findCell(r, c);

        cell.removeCell();
        this.cells.remove(cell);
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

    private boolean isValid(int r, int c) {
        return ((0 <= r && r < this.height) && (0 <= c && c < this.width));
    }

    private boolean isEmptyCell(int r, int c) {
        return isValid(r, c) && charAt(r, c) != '#';
    }

    private char charAt(int r, int c) {
        if (r < 0 || r >= this.height) {
            throw new IndexOutOfBoundsException("Invalid row");
        }
        if (c < 0 || c >= this.width) {
            throw new IndexOutOfBoundsException("Invalid column");
        }
        return this.mapChars[r][c];
    }

    @Override
    public String toString() {
        int vPad = (this.height - 1) * 3;
        int hPad = (this.width - 1) * 3;

        int oHeight = this.height + vPad;
        int oWidth = this.width + hPad;

        char[][] output = new char[oHeight][oWidth];

        for (int r = 0; r < oHeight; ++r) {
            for (int c = 0; c < oWidth; ++c) {
                output[r][c] = ' ';
            }
        }

        // add the '|' cells
        for (int r = 0; r < this.height - 1; ++r) {
            int rInd = 2 + (4 * r);
            for (int c = 0; c < this.width; ++c) {
                int cInd = 4 * c;
                output[rInd][cInd] = '|';
            }
        }

        // add the '-' cells
        for (int c = 0; c < this.width - 1; ++c) {
            int cInd = 2 + (4 * c);
            for (int r = 0; r < this.height; ++r) {
                int rInd = 4 * r;
                output[rInd][cInd] = '-';
            }
        }

        // check for connections
        for (int r = 0; r < this.height; ++r) {
            int rInd = 4 * r;
            for (int c = 0; c < this.width; ++c) {
                int cInd = 4 * c;

                char cellChar = this.charAt(r, c);
                output[rInd][cInd] = cellChar;

                if (cellChar != '#') {
                    Cell cell = this.findCell(r, c);
                    output[rInd][cInd] = cell.value == -1 ? 'x' : Integer.toHexString(cell.value).charAt(0);

                    if (isEmptyCell(r - 1, c)) {
                        Cell other = this.findCell(r - 1, c);
                        if (cell.outConnections.contains(other)) {
                            output[rInd - 4 + 1][cInd] = '^';
                        }
                    }
                    if (isEmptyCell(r, c + 1)) {
                        Cell other = this.findCell(r, c + 1);
                        if (cell.outConnections.contains(other)) {
                            output[rInd][cInd + 4 - 1] = '>';
                        }
                    }
                    if (isEmptyCell(r + 1, c)) {
                        Cell other = this.findCell(r + 1, c);
                        if (cell.outConnections.contains(other)) {
                            output[rInd + 4 - 1][cInd] = 'v';
                        }
                    }
                    if (isEmptyCell(r, c - 1)) {
                        Cell other = this.findCell(r, c - 1);
                        if (cell.outConnections.contains(other)) {
                            output[rInd][cInd - 4 + 1] = '<';
                        }
                    }
                }
            }
        }

        return Arrays.stream(output).map(String::new).collect(Collectors.joining("\n"));
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

        public void removeCell() {
            Set<Cell> deps = cellsDependingOnMe();
            deps.forEach(cell -> {
                cell.valueCells.clear();
                cell.value = -1;
            });

            this.valueCells.clear();
            this.value = -1;

            this.outConnections.forEach(out -> out.inConnections.remove(this));

            Set<Cell> toProcess = new HashSet<>(cells);
            while (!toProcess.isEmpty()) {
                toProcess = toProcess.stream()
                        .map(Cell::checkValues)
                        .flatMap(Set::stream)
                        .collect(Collectors.toSet());
            }
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

        private Set<Cell> cellsDependingOnMe() {
            Set<Cell> deps = new HashSet<>();

            LinkedList<Cell> toProcess = new LinkedList<>();
            this.outConnections
                    .stream()
                    .filter(out -> out.valueCells.contains(this))
                    .forEach(toProcess::push);

            while (!toProcess.isEmpty()) {
                Cell cell = toProcess.pop();
                if (deps.add(cell)) {
                    Set<Cell> poweredByCell = cell.outConnections
                            .stream()
                            .filter(out -> out.valueCells.contains(cell))
                            .collect(Collectors.toSet());

                    poweredByCell.forEach(toProcess::remove);
                    poweredByCell.forEach(toProcess::push);
                }
            }
            return deps;
        }

        public boolean canPower() {
            if (this.value != -1 && this.valueCells.isEmpty()) {
                return true;
            }

            Set<Cell> depthCells = new HashSet<>();
            LinkedList<Cell> processList = new LinkedList<>();
            processList.push(this);

            while (!processList.isEmpty()) {
                Cell cell = processList.pop();
                if (depthCells.add(cell)) {
                    cell.valueCells.forEach(processList::remove);
                    cell.valueCells.forEach(processList::push);
                }
            }
            return depthCells.stream().anyMatch(cell -> cell.value != -1 && cell.valueCells.isEmpty());
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
