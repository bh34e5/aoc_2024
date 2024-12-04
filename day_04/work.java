import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Work {
    public static void main(String[] args) {
        String filename = args[0];

        List<String> lines = null;
        try (Stream<String> linesStream = Files.lines(Path.of(filename))) {
            lines = linesStream.collect(Collectors.toList());
        } catch (IOException e) {
            System.err.println("Could not open file");
            System.exit(1);
        }

        int part = 1;
        if (args.length > 1) {
            try {
                part = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignore) {
            }
        }

        switch (part) {
            case 1:
                partOne(lines);
                break;
            case 2:
                partTwo(lines);
                break;
            default:
                System.err.println("Unknown part");
                System.exit(1);
        }
    }

    private static void partOne(List<String> lines) {
        int count = 0;

        Viewer v = new Viewer(lines);
        for (int x = 0; x < v.width; ++x) {
            for (int y = 0; y < v.height; ++y) {
                if (v.at(x, y) != 'X')
                    continue;

                final int cx = x;
                final int cy = y;
                count += Viewer.Direction.countMatch(dir -> v.inDirection(dir, cx, cy, 4).equals("XMAS"));
            }
        }

        System.out.println("Count: " + count);
    }

    private static void partTwo(List<String> lines) {
        int count = 0;

        Viewer v = new Viewer(lines);
        for (int x = 0; x < v.width; ++x) {
            for (int y = 0; y < v.height; ++y) {
                if (v.at(x, y) != 'A')
                    continue;

                final int cx = x;
                final int cy = y;
                int matched = Viewer.Direction.countMatch(false, dir -> {
                    Viewer.Direction opposite = dir.opposite();
                    int cdx = opposite.nextX(cx);
                    int cdy = opposite.nextY(cy);
                    return v.validPoint(cdx, cdy) && v.inDirection(dir, cdx, cdy, 3).equals("MAS");
                });

                if (matched == 2) {
                    count += 1;
                }
            }
        }

        System.out.println("Count: " + count);
    }

    static class Viewer {
        private final List<String> lines;
        private final int height;
        private final int width;

        public Viewer(List<String> lines) {
            validateLines(lines);
            this.lines = lines;
            this.height = lines.size();
            this.width = lines.isEmpty() ? 0 : lines.get(0).length();
        }

        private void validateLines(List<String> lines) {
            Objects.requireNonNull(lines);
            if (lines.isEmpty())
                return;

            int len = lines.get(0).length();
            if (!lines.stream().allMatch(s -> s.length() == len)) {
                throw new IllegalArgumentException("Non-rectangular grid");
            }
        }

        private boolean validPoint(int x, int y) {
            return (0 <= x && x < this.width) && (0 <= y && y < this.height);
        }

        private char at(int x, int y) {
            if (x < 0 || x >= this.width)
                throw new IndexOutOfBoundsException("x is invalid");
            if (y < 0 || y >= this.height)
                throw new IllegalArgumentException("y is invalid");

            return this.lines.get(y).charAt(x);
        }

        private String inDirection(Direction dir, int x, int y, int len) {
            Objects.requireNonNull(dir);
            if (len < 0)
                throw new IllegalArgumentException("Length must be non-negative");
            if (x < 0 || x >= this.width)
                throw new IndexOutOfBoundsException("x is invalid");
            if (y < 0 || y >= this.height)
                throw new IllegalArgumentException("y is invalid");

            if (len == 0)
                return "";

            char[] chars = new char[len];
            int i = 0;
            int curX = x;
            int curY = y;
            while (i < len && validPoint(curX, curY)) {
                chars[i] = this.at(curX, curY);
                i = i + 1;
                curX = dir.nextX(curX);
                curY = dir.nextY(curY);
            }

            return new String(chars, 0, i);
        }

        enum Direction {
            NORTH(true),
            SOUTH(true),
            EAST(true),
            WEST(true),
            NORTHEAST(false),
            SOUTHEAST(false),
            NORTHWEST(false),
            SOUTHWEST(false);

            private final boolean cardinal;

            Direction(boolean cardinal) {
                this.cardinal = cardinal;
            }

            public Direction opposite() {
                return switch (this) {
                    case NORTH -> SOUTH;
                    case SOUTH -> NORTH;
                    case EAST -> WEST;
                    case WEST -> EAST;
                    case NORTHEAST -> SOUTHWEST;
                    case SOUTHEAST -> NORTHWEST;
                    case NORTHWEST -> SOUTHEAST;
                    case SOUTHWEST -> NORTHEAST;
                };
            }

            public int nextX(int x) {
                return switch (this) {
                    case NORTH, SOUTH -> x;
                    case EAST, NORTHEAST, SOUTHEAST -> x + 1;
                    case WEST, NORTHWEST, SOUTHWEST -> x - 1;
                };
            }

            public int nextY(int y) {
                return switch (this) {
                    case EAST, WEST -> y;
                    case NORTH, NORTHEAST, NORTHWEST -> y - 1;
                    case SOUTH, SOUTHEAST, SOUTHWEST -> y + 1;
                };
            }

            public static int countMatch(Predicate<Direction> test) {
                int match = 0;
                for (Direction d : values()) {
                    if (test.test(d)) {
                        match += 1;
                    }
                }
                return match;
            }

            public static int countMatch(boolean cardinalOnly, Predicate<Direction> test) {
                int match = 0;
                for (Direction d : values()) {
                    if (cardinalOnly == d.cardinal && test.test(d)) {
                        match += 1;
                    }
                }
                return match;
            }
        }
    }
}
