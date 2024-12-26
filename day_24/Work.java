import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Work {
  public static void main(String[] args) throws IOException {
    if (args.length < 1) {
      throw new IllegalStateException("Expected filename");
    }

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
      case 1 -> partOne(contents);
      case 2 -> partTwo(contents);
      default -> throw new IllegalArgumentException("Bad part");
    }
  }

  private static void partOne(String contents) {
    Map<String, Wire> wires = new HashMap<>();
    List<Gate> gates = new ArrayList<>();

    assembleCircuit(contents, wires, gates);

    Map<Integer, Wire> wireByNum = wires.values()
        .stream()
        .filter(w -> isOutputWireName(w.name))
        .collect(Collectors.toMap(w -> outputWireNum(w.name), Function.identity()));

    TreeSet<Integer> keys = new TreeSet<>(Comparator.<Integer>naturalOrder().reversed());
    keys.addAll(wireByNum.keySet());

    long output = 0;
    for (Integer i : keys) {
      Wire w = wireByNum.get(i);
      output = (2 * output) + (w.getValue() ? 1 : 0);
    }

    System.out.println("Output = " + output);
  }

  private static void partTwo(String contents) throws IOException {
    Map<String, Wire> wires = new HashMap<>();
    List<Gate> gates = new ArrayList<>();

    assembleCircuit(contents, wires, gates);

    File file = new File("graph_pos.dot");
    file.createNewFile();

    try (PrintStream ps = new PrintStream(file)) {
      ps.println("digraph {");

      for (Gate g : gates) {
        Wire left = g.leftWire();
        Wire right = g.rightWire();
        Wire output = g.outputWire();

        final int WIDTH = 2;
        final int HEIGHT = -2;

        if (isWireName('x', left.name)) {
          int num = wireNum('x', left.name);
          int xPos = (1 + 2 * num) * WIDTH;
          int yPos = (0 + 2 * num) * HEIGHT;

          ps.println(String.format("%s [pos=\"%d,%d!\"];", left.name, xPos, yPos));
        } else if (isWireName('y', left.name)) {
          int num = wireNum('y', left.name);
          int xPos = (2 + 2 * num) * WIDTH;
          int yPos = (0 + 2 * num) * HEIGHT;

          ps.println(String.format("%s [pos=\"%d,%d!\"];", left.name, xPos, yPos));
        }

        if (isWireName('x', right.name)) {
          int num = wireNum('x', right.name);
          int xPos = (1 + 2 * num) * WIDTH;
          int yPos = (0 + 2 * num) * HEIGHT;

          ps.println(String.format("%s [pos=\"%d,%d!\"];", right.name, xPos, yPos));
        } else if (isWireName('y', right.name)) {
          int num = wireNum('y', right.name);
          int xPos = (2 + 2 * num) * WIDTH;
          int yPos = (0 + 2 * num) * HEIGHT;

          ps.println(String.format("%s [pos=\"%d,%d!\"];", right.name, xPos, yPos));
        }

        if (isWireName('x', left.name)
            || isWireName('y', left.name)
            || isWireName('x', right.name)
            || isWireName('y', right.name)) {
          int num = isWireName('x', left.name) ? wireNum('x', left.name) : wireNum('y', left.name);
          int xPos, yPos;
          if (g.getType().equals("AND")) {
            xPos = (2 + 2 * num) * WIDTH;
            yPos = (1 + 2 * num) * HEIGHT;
          } else {
            xPos = (1 + 2 * num) * WIDTH;
            yPos = (1 + 2 * num) * HEIGHT;
          }
          ps.println(String.format("%s [pos=\"%d,%d!\"];", output.name, xPos, yPos));
        }

        ps.println(String.format("%s -> %s;", left.name, output.name));
        ps.println(String.format("%s -> %s;", right.name, output.name));

        if (isOutputWireName(output.name)) {
          int num = outputWireNum(output.name);
          int xPos = (0 + 2 * num) * WIDTH;
          int yPos = (2 + 2 * num) * HEIGHT;

          ps.println(
              String.format("%s [label=\"%s\\n%s\"; pos=\"%d,%d!\"];",
                  output.name, g.getType(), output.name, xPos, yPos));
        } else {
          ps.println(String.format("%s [label=\"%s\\n%s\"];", output.name, g.getType(), output.name));
        }
      }

      ps.println("}");
    }
    System.out.println("Run the following command, then check the graph PNG for errors.");
    System.out.println("dot -Tpng -Kfdp -o graph_pos.png graph_pos.dot");
  }

  private static void assembleCircuit(String contents, Map<String, Wire> wires, List<Gate> gates) {
    String[] pieces = contents.split("\n\n");
    String powers = pieces[0];
    String combinations = pieces[1];

    Pattern wirePattern = Pattern.compile("(\\w+) ([A-Z]+) (\\w+) -> (\\w+)");
    String[] wireLines = combinations.strip().split("\n");

    for (String combination : wireLines) {
      Matcher matcher = wirePattern.matcher(combination);
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Invalid wire description");
      }

      String strLeft = matcher.group(1);
      String strRight = matcher.group(3);
      String strOutput = matcher.group(4);
      String strGate = matcher.group(2);

      Wire left = wires.computeIfAbsent(strLeft, l -> new Wire(l));
      Wire right = wires.computeIfAbsent(strRight, r -> new Wire(r));
      Wire output = wires.computeIfAbsent(strOutput, o -> new Wire(o));

      Gate gate = Gate.fromType(strGate, left, right, output);
      gates.add(gate);
    }

    Pattern powerPattern = Pattern.compile("(\\w+): (\\d)");
    String[] powerLines = powers.strip().split("\n");

    for (String power : powerLines) {
      Matcher matcher = powerPattern.matcher(power);
      if (!matcher.matches()) {
        throw new IllegalArgumentException("Invalid power description");
      }

      String strWire = matcher.group(1);
      String strPower = matcher.group(2);

      if (!(strPower.equals("0") || strPower.equals("1"))) {
        throw new IllegalArgumentException("Invalid power description");
      }

      Wire wire = wires.get(strWire);
      if (wire == null) {
        throw new IllegalArgumentException("Unknown wire");
      }

      wire.powerWith(strPower.equals("1"));
    }
  }

  private static int outputWireNum(String s) {
    return wireNum('z', s);
  }

  private static int wireNum(char prefix, String s) {
    Pattern p = Pattern.compile(prefix + "(\\d+)");
    Matcher m = p.matcher(s);

    if (!m.matches()) {
      throw new IllegalArgumentException("Invalid output wire string");
    }
    return Integer.parseInt(m.group(1));
  }

  private static boolean isWireName(char prefix, String s) {
    return Pattern.matches(prefix + "\\d+", s);
  }

  private static boolean isOutputWireName(String s) {
    return isWireName('z', s);
  }
}

abstract class Gate {
  public abstract void notifyValue();

  public abstract String getType();

  public abstract Wire leftWire();

  public abstract Wire rightWire();

  public abstract Wire outputWire();

  public static Gate fromType(String type, Wire left, Wire right, Wire output) {
    return switch (type.toUpperCase()) {
      case "AND" -> new AndGate(left, right, output);
      case "OR" -> new OrGate(left, right, output);
      case "XOR" -> new XorGate(left, right, output);
      default -> throw new IllegalArgumentException("Unknown type");
    };
  }

  private static class AndGate extends Gate {
    Wire left;
    Wire right;
    Wire output;

    private AndGate(Wire left, Wire right, Wire output) {
      this.left = left;
      this.right = right;
      this.output = output;

      left.addOutput(this);
      right.addOutput(this);
    }

    @Override
    public String getType() {
      return "AND";
    }

    @Override
    public void notifyValue() {
      if (this.left.isPowered() && this.right.isPowered()) {
        this.output.powerWith(this.left.getValue() && this.right.getValue(), this);
      } else {
        this.output.reset();
      }
    }

    @Override
    public Wire leftWire() {
      return this.left;
    }

    @Override
    public Wire rightWire() {
      return this.right;
    }

    @Override
    public Wire outputWire() {
      return this.output;
    }
  }

  private static class OrGate extends Gate {
    Wire left;
    Wire right;
    Wire output;

    private OrGate(Wire left, Wire right, Wire output) {
      this.left = left;
      this.right = right;
      this.output = output;

      left.addOutput(this);
      right.addOutput(this);
    }

    @Override
    public String getType() {
      return "OR";
    }

    @Override
    public void notifyValue() {
      if (this.left.isPowered() && this.right.isPowered()) {
        this.output.powerWith(this.left.getValue() || this.right.getValue(), this);
      } else {
        this.output.reset();
      }
    }

    @Override
    public Wire leftWire() {
      return this.left;
    }

    @Override
    public Wire rightWire() {
      return this.right;
    }

    @Override
    public Wire outputWire() {
      return this.output;
    }
  }

  private static class XorGate extends Gate {
    Wire left;
    Wire right;
    Wire output;

    private XorGate(Wire left, Wire right, Wire output) {
      this.left = left;
      this.right = right;
      this.output = output;

      left.addOutput(this);
      right.addOutput(this);
    }

    @Override
    public String getType() {
      return "XOR";
    }

    @Override
    public void notifyValue() {
      if (this.left.isPowered() && this.right.isPowered()) {
        this.output.powerWith(Boolean.logicalXor(this.left.getValue(), this.right.getValue()), this);
      } else {
        this.output.reset();
      }
    }

    @Override
    public Wire leftWire() {
      return this.left;
    }

    @Override
    public Wire rightWire() {
      return this.right;
    }

    @Override
    public Wire outputWire() {
      return this.output;
    }
  }
}

class Wire {
  final String name;
  private boolean powered;
  private boolean value;

  private Gate source;
  private final List<Gate> outputs;

  public Wire(String name) {
    this.name = name;
    this.powered = false;
    this.value = false;
    this.source = null;
    this.outputs = new ArrayList<>();
  }

  public Gate getSource() {
    return this.source;
  }

  public List<Gate> outputGates() {
    return this.outputs;
  }

  public void addOutput(Gate output) {
    this.outputs.add(output);
  }

  public boolean isPowered() {
    return this.powered;
  }

  public boolean getValue() {
    if (!isPowered()) {
      throw new IllegalStateException("Getting value of non-powered wire");
    }
    return this.value;
  }

  public void powerWith(boolean value) {
    powerWith(value, null);
  }

  public void powerWith(boolean value, Gate source) {
    this.powered = true;
    this.source = source;
    this.value = value;

    this.outputs.forEach(Gate::notifyValue);
  }

  public void reset() {
    this.powered = false;
  }
}
