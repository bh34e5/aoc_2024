import functools
import re
import sys


def read_text(filename):
    with open(filename, 'r') as f:
        lines = f.readlines()
        split_lines = list(map(lambda l: list(map(int,
                                                  re.split(r'\s+',
                                                           l.strip()))),
                               lines))
        return split_lines


def part_one(lines):
    left, right = zip(*lines)
    s_left = sorted(left)
    s_right = sorted(right)

    diffs = map(lambda l, r: abs(l - r), s_left, s_right)
    return functools.reduce(lambda tot, cur: tot + cur, diffs, 0)


def part_two(lines):
    left, right = zip(*lines)
    r_counts = {}
    for r in right:
        r_counts[r] = r_counts.get(r, 0) + 1

    res = 0
    for l in left:
        res += l * r_counts.get(l, 0)
    return res


if __name__ == '__main__':
    filename = sys.argv[1]
    split_text = read_text(filename)

    if len(sys.argv) > 2:
        res = part_two(split_text)
    else:
        res = part_one(split_text)

    print(res)
