import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Work {
    public static void main(String[] args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Expected filename");
        }

        String filename = args[0];
        int part = 1;

        if (args.length > 1) {
            try {
                part = Integer.parseInt(args[1]);
            } catch (NumberFormatException ignore) {
            }
        }

        try {
            Path filePath = Path.of(filename);
            String contents = Files.readString(filePath);

            switch (part) {
                case 1:
                    partOne(contents);
                    break;
                case 2:
                    partTwo(contents);
                    break;
                default:
                    throw new IllegalStateException("Bad part");
            }
        } catch (IOException io) {
            System.err.println("Failed to open file: " + filename);
            System.exit(1);
        }
    }

    private static long uncheckedGcd(long a, long b) {
        if (a % b == 0) {
            return b;
        }
        return uncheckedGcd(b, a % b);
    }

    public static long gcd(long a, long b) {
        if (a < 0) {
            throw new IllegalArgumentException("a must be positive");
        }
        if (b < 0) {
            throw new IllegalArgumentException("b must be positive");
        }
        if (a == 0 && b == 0) {
            throw new IllegalArgumentException("Both cannot be zero");
        }
        if (a == 0) {
            return b;
        }
        if (b == 0) {
            return a;
        }
        return uncheckedGcd(a, b);
    }

    private static class Frac {
        public final long num;
        public final long den;

        public Frac(long num) {
            this.num = num;
            this.den = 1;
        }

        public Frac(long num, long den) {
            long g = gcd(Math.abs(num), Math.abs(den)); // always lowest terms
            long sign = den < 0 ? -1 : 1; // always den is pos
            this.num = sign * num / g;
            this.den = sign * den / g;
        }

        public Frac mul(Frac other) {
            return new Frac(this.num * other.num, this.den * other.den);
        }

        public Frac add(Frac other) {
            return new Frac(this.num * other.den + this.den * other.num, this.den * other.den);
        }

        public Frac addInv() {
            return new Frac(-this.num, this.den);
        }

        public Frac mulInv() {
            return new Frac(this.den, this.num);
        }

        public boolean isPositive() {
            return this.num > 0;
        }

        public boolean isZero() {
            return this.num == 0;
        }

        public boolean isIntegral() {
            return this.den == 1 || isZero();
        }

        public long asLong() {
            if (!isIntegral()) {
                throw new IllegalStateException("Long of non-integral frac");
            }
            if (isZero()) {
                return 0;
            }
            return this.num;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Frac &&
                    (((Frac) other).num == this.num) &&
                    (((Frac) other).den == this.den);
        }

        @Override
        public String toString() {
            if (isZero()) {
                return "0";
            } else if (isIntegral()) {
                return Long.toString(this.num);
            }
            return Long.toString(this.num) + "/" + Long.toString(this.den);
        }
    }

    private static class Vec {
        public final Frac a;
        public final Frac b;

        public Vec(Frac a, Frac b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public String toString() {
            return "<" + this.a + ", " + this.b + ">";
        }
    }

    private static class Buttons {
        public final Frac a;
        public final Frac b;
        public final Frac c;
        public final Frac d;

        public Buttons(Frac a, Frac b, Frac c, Frac d) {
            this.a = a;
            this.b = b;
            this.c = c;
            this.d = d;
        }

        public boolean canMakeTarget(Vec target) {
            if (!invertible()) {
                return false;
            }

            Vec clicks = getInverse().apply(target);
            return clicks.a.isIntegral() &&
                    clicks.b.isIntegral() &&
                    (clicks.a.isZero() || clicks.a.isPositive()) &&
                    (clicks.b.isZero() || clicks.b.isPositive());
        }

        private Vec get(Vec target) {
            if (!invertible()) {
                throw new IllegalStateException("Inverse of non-invertible matrix");
            }
            return getInverse().apply(target);
        }

        public Vec apply(Vec in) {
            return new Vec(
                    this.a.mul(in.a).add(this.b.mul(in.b)),
                    this.c.mul(in.a).add(this.d.mul(in.b)));
        }

        private Frac det() {
            return a.mul(d).add(b.mul(c).addInv());
        }

        private boolean invertible() {
            return !det().isZero();
        }

        private Buttons getInverse() {
            Frac dInv = det().mulInv();
            return new Buttons(
                    this.d.mul(dInv),
                    this.b.mul(dInv).addInv(),
                    this.c.mul(dInv).addInv(),
                    this.a.mul(dInv));
        }

        @Override
        public String toString() {
            return "[[" + this.a + ", " + this.b + "], [" + this.c + ", " + this.d + "]]";
        }
    }

    public static void partOne(String contents) {
        long offset = 0;
        long minCost = getMinCost(contents, offset);

        System.out.println(minCost);
    }

    public static void partTwo(String contents) {
        long offset = Long.parseLong("10000000000000");
        long minCost = getMinCost(contents, offset);

        System.out.println(minCost);
    }

    private static long getMinCost(String contents, long offset) {
        String[] parts = contents.split("\n\n");
        Pattern pattern = Pattern.compile("Button A: X\\+(\\d+), Y\\+(\\d+)\n" +
                "Button B: X\\+(\\d+), Y\\+(\\d+)\n" +
                "Prize: X=(\\d+), Y=(\\d+)");

        final long A_COST = 3;
        final long B_COST = 1;

        long total = 0;
        for (String part : parts) {
            Matcher matcher = pattern.matcher(part.strip());
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Invalid input");
            }

            Buttons buttons = new Buttons(
                    new Frac(Long.parseLong(matcher.group(1))),
                    new Frac(Long.parseLong(matcher.group(3))),
                    new Frac(Long.parseLong(matcher.group(2))),
                    new Frac(Long.parseLong(matcher.group(4))));
            Frac targetX = new Frac(offset + Long.parseLong(matcher.group(5)));
            Frac targetY = new Frac(offset + Long.parseLong(matcher.group(6)));
            Vec targetVec = new Vec(targetX, targetY);

            if (buttons.canMakeTarget(targetVec)) {
                Vec clicks = buttons.get(targetVec);
                // we know these are integral (or zero)
                total += A_COST * clicks.a.asLong() + B_COST * clicks.b.asLong();
            } else {
                long minPrice = Long.MAX_VALUE;

                Frac targetSlope = targetVec.a.mul(targetVec.b.mulInv());
                Frac aSlope = buttons.a.mul(buttons.c.mulInv());
                Frac bSlope = buttons.b.mul(buttons.d.mulInv());

                if (targetSlope.equals(aSlope)) {
                    Frac ratA = targetVec.a.mul(buttons.a.mulInv());
                    if (ratA.isIntegral()) {
                        minPrice = Math.min(minPrice, A_COST * ratA.asLong());
                    }
                }
                if (targetSlope.equals(bSlope)) {
                    Frac ratB = targetVec.b.mul(buttons.b.mulInv());
                    if (ratB.isIntegral()) {
                        minPrice = Math.min(minPrice, B_COST * ratB.asLong());
                    }
                }

                if (minPrice != Long.MAX_VALUE) {
                    total += minPrice;
                }
            }
        }

        return total;
    }
}
