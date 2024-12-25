import sys
from typing import Generator, Iterable


def get_verts(pairs: Iterable[tuple[str, str]]) -> set[str]:
    s = set()
    for v1, v2 in pairs:
        s.add(v1)
        s.add(v2)
    return s


def get_edges(pairs: Iterable[tuple[str, str]]) -> set[tuple[str, str]]:
    s = set()
    for v1, v2 in pairs:
        if v1 < v2:
            s.add((v1, v2))
        else:
            s.add((v2, v1))
    return s


def iter_has_t(ps: Iterable[str]) -> bool:
    for p in ps:
        if p.startswith('t'):
            return True
    return False


def get_pair(line: str) -> tuple[str, str]:
    sp = line.split('-')
    assert len(sp) == 2
    return sp[0], sp[1]


def k_tuples(items: list[str], k: int) -> Generator[list[str], None, None]:
    if k == 1:
        for item in items:
            yield [item]
    elif k == len(items):
        yield items
    elif k < len(items):
        for t in k_tuples(items[1:], k-1):
            yield [items[0], *t]
        yield from k_tuples(items[1:], k)


def is_k_clique(verts: list[str], edges: set[tuple[str, str]]) -> bool:
    pairs = k_tuples(verts, 2)
    return all([tuple(edge) in edges for edge in pairs])


def partOne(lines: list[str]) -> int:
    pairs = [get_pair(line) for line in lines]

    edges_with_ts = filter(iter_has_t, pairs)
    verts = get_verts(edges_with_ts)

    edges_in_verts = filter(lambda e: e[0] in verts or e[1] in verts, pairs)
    edges = get_edges(edges_in_verts)

    triples_with_ts = filter(iter_has_t, k_tuples(sorted(verts), 3))
    k_cliques = filter(lambda test: is_k_clique(test, edges), triples_with_ts)
    return len(list(k_cliques))


def bron_kerbosch(verts: set[str], edges: set[tuple[str, str]]) -> set[str]:
    results = []

    vert_to_edge = {v: set() for v in verts}
    for v1, v2 in edges:
        vert_to_edge[v1].add(v2)
        vert_to_edge[v2].add(v1)

    def inner(r: set[str], p: set[str], x: set[str]) -> Generator[set[str], None, None]:
        call_stack = [(r, p, x)]
        while call_stack:
            frame = call_stack.pop()
            r, p, x = frame

            if not p and not x:
                yield r
            else:
                pivot_options = p.copy()
                pivot_options.update(x)
                u = list(pivot_options)[0]

                vertex_options = p.copy() - vert_to_edge[u]
                for v in vertex_options:
                    new_r = r.copy()
                    new_p = p.copy()
                    new_x = x.copy()

                    new_r.add(v)
                    new_p = new_p.intersection(vert_to_edge[v])
                    new_x = new_x.intersection(vert_to_edge[v])

                    call_stack.append((new_r, new_p, new_x))

                    p.remove(v)
                    x.add(v)

    results = [clique for clique in inner(set(), verts.copy(), set())]
    return max(results, key=lambda s: len(s))


def partTwo(lines: list[str]) -> int:
    pairs = [get_pair(line) for line in lines]

    verts = get_verts(pairs)
    edges = get_edges(pairs)

    max_clique = bron_kerbosch(verts, edges)
    print(",".join(sorted(max_clique)))

    return 0  # return value is actually the print above


if __name__ == '__main__':
    args = sys.argv[1:]

    if not args:
        raise ValueError("Expected filename")
    [filename, *args] = args

    if args:
        part = int(args[0])
    else:
        part = 1

    with open(filename, 'r') as f:
        content = f.read()

    lines = content.splitlines()
    if part == 1:
        print(partOne(lines))
    elif part == 2:
        print(partTwo(lines))
    else:
        raise ValueError("Invalid part")
