import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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
        List<List<Integer>> nums = lines.stream()
                .map(l -> Arrays.stream(l.split("")).map(Integer::parseInt).collect(Collectors.toList()))
                .collect(Collectors.toList());

        int rows = nums.size();
        int cols = nums.get(0).size();
        nums.forEach(l -> {
            if (l.size() != cols) {
                throw new IllegalStateException();
            }
        });

        List<Pair<Integer, Integer>> coords = getCoords(rows, cols);
        coords.sort((p1, p2) -> {
            int n1 = nums.get(p1.lft).get(p1.rgt);
            int n2 = nums.get(p2.lft).get(p2.rgt);
            return n2 - n1; // sort by descending values
        });

        List<List<Set<Pair<Integer, Integer>>>> results = IntStream.range(0, rows).boxed().map(
                r -> IntStream.range(0, cols).boxed()
                        .map(c -> (Set<Pair<Integer, Integer>>) new HashSet<Pair<Integer, Integer>>())
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        Processor<Integer, Integer, Integer, Set<Pair<Integer, Integer>>> processPoint = (t, r, c) -> {
            if ((0 <= r && r < rows) && (0 <= c && c < cols)
                    && (nums.get(r).get(c) == t + 1)) {
                return results.get(r).get(c);
            }
            return null;
        };

        for (Pair<Integer, Integer> coord : coords) {
            int r = coord.lft;
            int c = coord.rgt;

            int thisVal = nums.get(r).get(c);
            Set<Pair<Integer, Integer>> thisResult = results.get(r).get(c);
            if (thisVal == 9) {
                thisResult.add(new Pair<Integer, Integer>(r, c));
            } else {
                Set<Pair<Integer, Integer>> n = processPoint.accept(thisVal, r - 1, c);
                Set<Pair<Integer, Integer>> e = processPoint.accept(thisVal, r, c + 1);
                Set<Pair<Integer, Integer>> s = processPoint.accept(thisVal, r + 1, c);
                Set<Pair<Integer, Integer>> w = processPoint.accept(thisVal, r, c - 1);

                if (n != null) {
                    thisResult.addAll(n);
                }
                if (e != null) {
                    thisResult.addAll(e);
                }
                if (s != null) {
                    thisResult.addAll(s);
                }
                if (w != null) {
                    thisResult.addAll(w);
                }
            }
        }

        Counter counter = new Counter();
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                System.out.printf("(%d)%03d ", nums.get(r).get(c), results.get(r).get(c).size());
                if (nums.get(r).get(c) == 0) {
                    counter.inc(results.get(r).get(c).size());
                }
            }
            System.out.println();
        }
        System.out.println("Total = " + counter.getInt());
    }

    static class Counter {
        private int c = 0;

        public void inc() {
            ++c;
        }

        public void inc(int n) {
            c += n;
        }

        public int getInt() {
            return c;
        }
    }

    @FunctionalInterface
    private interface Processor<T, U, V, R> {
        R accept(T t, U u, V v);
    }

    private static List<Pair<Integer, Integer>> getCoords(int rows, int cols) {
        return IntStream.range(0, rows).boxed().flatMap(
                r -> IntStream.range(0, cols).boxed().map(c -> new Pair<Integer, Integer>(r, c)))
                .collect(Collectors.toList());
    }

    private static class Pair<L, R> {
        public final L lft;
        public final R rgt;

        public Pair(L lft, R rgt) {
            this.lft = lft;
            this.rgt = rgt;
        }

        @Override
        public int hashCode() {
            return 13 * lft.hashCode() + 7 * rgt.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Pair && this.lft.equals(((Pair<?, ?>) other).lft)
                    && this.rgt.equals(((Pair<?, ?>) other).rgt);
        }
    }

    private static void partTwo(List<String> lines) {
        List<List<Integer>> nums = lines.stream()
                .map(l -> Arrays.stream(l.split("")).map(Integer::parseInt).collect(Collectors.toList()))
                .collect(Collectors.toList());

        int rows = nums.size();
        int cols = nums.get(0).size();
        nums.forEach(l -> {
            if (l.size() != cols) {
                throw new IllegalStateException();
            }
        });

        List<Pair<Integer, Integer>> coords = getCoords(rows, cols);
        coords.sort((p1, p2) -> {
            int n1 = nums.get(p1.lft).get(p1.rgt);
            int n2 = nums.get(p2.lft).get(p2.rgt);
            return n2 - n1; // sort by descending values
        });

        List<List<Integer>> results = IntStream.range(0, rows).boxed().map(
                r -> IntStream.range(0, cols).boxed()
                        .map(c -> 0)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());

        Processor<Integer, Integer, Integer, Integer> processPoint = (t, r, c) -> {
            if ((0 <= r && r < rows) && (0 <= c && c < cols)
                    && (nums.get(r).get(c) == t + 1)) {
                return results.get(r).get(c);
            }
            return null;
        };

        for (Pair<Integer, Integer> coord : coords) {
            int r = coord.lft;
            int c = coord.rgt;

            int thisVal = nums.get(r).get(c);
            int thisResult;
            if (thisVal == 9) {
                thisResult = 1;
            } else {
                Integer n = processPoint.accept(thisVal, r - 1, c);
                Integer e = processPoint.accept(thisVal, r, c + 1);
                Integer s = processPoint.accept(thisVal, r + 1, c);
                Integer w = processPoint.accept(thisVal, r, c - 1);

                int sum = 0;
                if (n != null) {
                    sum += n;
                }
                if (e != null) {
                    sum += e;
                }
                if (s != null) {
                    sum += s;
                }
                if (w != null) {
                    sum += w;
                }

                thisResult = sum;
            }
            results.get(r).set(c, thisResult);
        }

        Counter counter = new Counter();
        for (int r = 0; r < rows; ++r) {
            for (int c = 0; c < cols; ++c) {
                System.out.printf("(%d)%03d ", nums.get(r).get(c), results.get(r).get(c));
                if (nums.get(r).get(c) == 0) {
                    counter.inc(results.get(r).get(c));
                }
            }
            System.out.println();
        }
        System.out.println("Total = " + counter.getInt());
    }
}
