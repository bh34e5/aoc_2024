import sys


def read_map(lines):
    h = len(lines)
    w = len(lines[0].strip())

    res = {}
    for r, line in enumerate(lines):
        for c, ch in enumerate(line.strip()):
            if ch == '.': continue
            res[ch] = res.get(ch, [])
            res[ch].append((r, c))

    return h, w, res


def plus(p1, p2):
    return (p1[0]+p2[0], p1[1]+p2[1])


def negate(p1):
    return (-p1[0], -p1[1])


def antipoles(t):
    [p1, p2] = t

    a1 = plus(p2, plus(p2, negate(p1)))
    a2 = plus(p1, plus(p1, negate(p2)))
    return [a1, a2]


def freqs_from(h, w, p1, p2):
    off = plus(p2, negate(p1))

    next = p2
    while in_map(h, w, next):
        yield next
        next = plus(next, off)


def in_map(h, w, p):
    return 0 <= p[0] < h and 0 <= p[1] < w


def pairs(l):
    size = len(l)
    res = []
    for i in range(size):
        for j in range(i+1, size):
            res.append([l[i], l[j]])
    return res


def part_one(lines):
    h, w, m = read_map(lines)

    a = {ch: [antipoles(p) for p in pairs(points)] for ch, points in m.items()}

    res = set()
    for ch in a:
        for [a1, a2] in a[ch]:
            if in_map(h, w, a1): res.add(a1)
            if in_map(h, w, a2): res.add(a2)
    return len(res)


def part_two(lines):
    h, w, m = read_map(lines)

    a = {ch: [[*freqs_from(h, w, p1, p2), *freqs_from(h, w, p2, p1)]
              for [p1, p2] in pairs(points)]
         for ch, points in m.items()}

    res = set()
    for ch in a:
        for flist in a[ch]:
            res.update(flist)
    return len(res)


if __name__ == '__main__':
    args = sys.argv[1:]
    filename = args[0]
    with open(filename, 'r') as f:
        lines = f.readlines()

    part = 1 if len(args) < 2 else int(args[1])
    if part == 1: print(part_one(lines))
    elif part == 2: print(part_two(lines))
    else: raise ValueError("Invalid part", part)
