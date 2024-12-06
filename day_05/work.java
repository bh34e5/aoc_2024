import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        lines.stream().filter(line -> line.indexOf('|') >= 0).forEach(Work::processDependency);
        long res = lines.stream().filter(line -> line.length() > 0 && line.indexOf('|') < 0)
                .map(s -> Arrays.asList(s.split(",")).stream().map(Long::parseLong).collect(Collectors.toList()))
                .filter(Work::isValid)
                .mapToLong(Work::middle).sum();

        System.out.println("Res: " + res);
    }

    private static void partTwo(List<String> lines) {
        lines.stream().filter(line -> line.indexOf('|') >= 0).forEach(Work::processDependency);

        long res = lines.stream().filter(line -> line.length() > 0 && line.indexOf('|') < 0)
                .map(s -> Arrays.asList(s.split(",")).stream().map(Long::parseLong).collect(Collectors.toList()))
                .filter(Predicate.not(Work::isValid))
                .map(Work::toOrdered)
                .mapToLong(Work::middle).sum();

        System.out.println("Res: " + res);
    }

    private static void processDependency(String line) {
        String[] parts = line.split("\\|");

        if (parts.length != 2) {
            throw new IllegalStateException("More than one thing...");
        }

        long fst = Long.parseLong(parts[0]);
        long snd = Long.parseLong(parts[1]);

        Dependency.forNumber(snd).mustComeAfter(Dependency.forNumber(fst));
    }

    private static boolean isValid(List<Long> numbers) {
        int size = numbers.size();
        for (int i = 0; i < size; ++i) {
            long iNum = numbers.get(i);
            for (int j = i + 1; j < size; ++j) {
                long jNum = numbers.get(j);
                if (Dependency.forNumber(iNum).doesComeAfter(Dependency.forNumber(jNum))) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<Long> toOrdered(List<Long> in) {
        int size = in.size();
        List<Long> res = new ArrayList<>(size);

        for (int i = 0; i < size; ++i) {
            res.add(i, in.get(i));
            for (int j = i; j >= 0; --j) {
                for (int k = 0; k < j && !isValid(res.subList(0, i + 1)); ++k) {
                    long tmp = res.get(j - k);
                    res.set(j - k, res.get(j - (k + 1)));
                    res.set(j - (k + 1), tmp);
                }
            }
        }
        return res;
    }

    private static long middle(List<Long> numbers) {
        int size = numbers.size();
        if (size % 2 == 0) {
            throw new IllegalStateException("Even sized list");
        }
        return numbers.get((size - 1) / 2);
    }

    static class Dependency {
        private static final Map<Long, Dependency> allDeps = new HashMap<>();

        private final long number; // this number
        private final Set<Long> dependencies; // the numbers that it has to come after

        private Dependency(long number) {
            this.number = number;
            this.dependencies = new HashSet<>();
        }

        public static Dependency forNumber(long number) {
            return allDeps.computeIfAbsent(number, Dependency::new);
        }

        public boolean doesComeAfter(Dependency other) {
            return this.dependencies.contains(other.number);
        }

        public void mustComeAfter(Dependency other) {
            this.dependencies.add(other.number);
        }
    }
}
