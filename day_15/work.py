import sys
from copy import deepcopy
from typing import List, Tuple


def map_at(map: List[List[str]], pos: Tuple[int, int]) -> str:
    r, c = pos
    return map[r][c]


def map_set(map: List[List[str]], pos: Tuple[int, int], s: str):
    r, c = pos
    map[r][c] = s


def tup_add(t1: Tuple[int, int], t2: Tuple[int, int]) -> Tuple[int, int]:
    x1, y1 = t1
    x2, y2 = t2
    return x1 + x2, y1 + y2


def move_in_dir_1(dir: Tuple[int, int], map: List[List[str]],
                  cur_pos: Tuple[int, int]) -> bool:
    next_pos = tup_add(cur_pos, dir)
    next_char = map_at(map, next_pos)
    if next_char == '#':
        return False  # wall, can't move
    elif next_char == '.':
        map_set(map, next_pos, map_at(map, cur_pos))
        map_set(map, cur_pos, '.')
        return True
    elif next_char == 'O':
        moved = move_in_dir_1(dir, map, next_pos)
        if moved:
            if map_at(map, next_pos) != '.':
                raise ValueError("Unexpected obstacle in move")

            map_set(map, next_pos, map_at(map, cur_pos))
            map_set(map, cur_pos, '.')
        return moved
    else:
        raise ValueError("Unexpected next_char", next_char)


def make_move_1(move: str, map: List[List[str]],
                cur_pos: Tuple[int, int]) -> Tuple[int, int]:
    if move == 'v':
        dir = 1, 0
    elif move == '^':
        dir = -1, 0
    elif move == '<':
        dir = 0, -1
    elif move == '>':
        dir = 0, 1
    else:
        raise ValueError("Invalid move")

    if map_at(map, cur_pos) != '@':
        raise ValueError("Invalid cur_pos")

    moved = move_in_dir_1(dir, map, cur_pos)
    return tup_add(cur_pos, dir) if moved else cur_pos


def partOne(contents: str):
    [map_str, moves_str] = contents.split("\n\n")
    map = [list(s) for s in map_str.split("\n")]
    moves = list(moves_str.replace("\n", ""))

    cur_pos = None
    for r, row in enumerate(map):
        for c, char in enumerate(row):
            if char == '@':
                if cur_pos is not None:
                    raise ValueError("Two start positions")
                else:
                    cur_pos = r, c
    if cur_pos is None:
        raise ValueError("No start position")

    for move in moves:
        cur_pos = make_move_1(move, map, cur_pos)

    print("\n".join(["".join(row) for row in map]))

    total = 0
    for r, row in enumerate(map):
        for c, char in enumerate(row):
            if char == 'O':
                total += 100 * r + c
    print(total)


def resize(map_str: str) -> str:
    return map_str\
        .replace('.', '..')\
        .replace('#', '##')\
        .replace('@', '@.')\
        .replace('O', '[]')


def get_nbr_prev(pos: Tuple[int, int]) -> Tuple[int, int]:
    return tup_add(pos, (0, -1))


def get_nbr(pos: Tuple[int, int]) -> Tuple[int, int]:
    return tup_add(pos, (0, 1))


def make_move_2(move: str, map: List[List[str]], cur_pos: Tuple[int, int]
                ) -> Tuple[List[List[str]], Tuple[int, int]]:
    def move_vert(dir: Tuple[int, int], map: List[List[str]],
                  cur: Tuple[int, int],
                  width: int) -> Tuple[List[List[str]], bool]:
        nbr = get_nbr(cur)
        cur_next = tup_add(cur, dir)
        nbr_next = tup_add(nbr, dir)

        if width == 1:
            cur_next_char = map_at(map, cur_next)
            if cur_next_char == '#':
                return map, False
            elif cur_next_char == '.':
                map_set(map, cur_next, map_at(map, cur))
                map_set(map, cur, '.')
                return map, True
            elif cur_next_char == '[':
                map, moved = move_vert(dir, map, cur_next, 2)
                if moved:
                    if map_at(map, cur_next) != '.':
                        raise ValueError("Unexpected obstacle in move")
                    if map_at(map, nbr_next) != '.':
                        raise ValueError("Unexpected obstacle in move")

                    map_set(map, cur_next, map_at(map, cur))
                    map_set(map, cur, '.')
                return map, moved
            elif cur_next_char == ']':
                next_to_move = tup_add(get_nbr_prev(cur), dir)
                map, moved = move_vert(dir, map, next_to_move, 2)
                if moved:
                    if map_at(map, next_to_move) != '.':
                        raise ValueError("Unexpected obstacle in move")
                    if map_at(map, cur_next) != '.':
                        raise ValueError("Unexpected obstacle in move")

                    map_set(map, cur_next, map_at(map, cur))
                    map_set(map, cur, '.')
                return map, moved
            else:
                raise ValueError("Unexpected cur_next_char", cur_next_char)
        elif width == 2:
            cur_next_char = map_at(map, cur_next)
            nbr_next_char = map_at(map, nbr_next)

            if cur_next_char == '#' or nbr_next_char == '#':
                return map, False

            map_copy = deepcopy(map)

            if cur_next_char == '[':
                map_copy, moved = move_vert(dir, map_copy, cur_next, 2)
                if moved:
                    if map_at(map_copy, cur_next) != '.':
                        raise ValueError("Unexpected obstacle in move")
                    if map_at(map_copy, nbr_next) != '.':
                        raise ValueError("Unexpected obstacle in move")

                    map_set(map_copy, cur_next, map_at(map_copy, cur))
                    map_set(map_copy, nbr_next, map_at(map_copy, nbr))
                    map_set(map_copy, cur, '.')
                    map_set(map_copy, nbr, '.')
                return map_copy, moved
            elif cur_next_char == ']':
                next_to_move = tup_add(get_nbr_prev(cur), dir)
                map_copy, moved = move_vert(dir, map_copy, next_to_move, 2)
                if not moved:
                    return map, False
            elif cur_next_char != '.':
                raise ValueError("Unexpected char")

            if nbr_next_char == '[':
                map_copy, moved = move_vert(dir, map_copy, nbr_next, 2)
                if not moved:
                    return map, False
            elif nbr_next_char != '.':
                raise ValueError("Unexpected char")

            if map_at(map_copy, cur_next) != '.':
                raise ValueError("Unexpected obstacle")
            if map_at(map_copy, nbr_next) != '.':
                raise ValueError("Unexpected obstacle")

            map_set(map_copy, cur_next, map_at(map_copy, cur))
            map_set(map_copy, nbr_next, map_at(map_copy, nbr))
            map_set(map_copy, cur, '.')
            map_set(map_copy, nbr, '.')

            return map_copy, True
        else:
            raise ValueError("Unexpected width")

    def move_left(cur: Tuple[int, int], width: int) -> bool:
        left_pos = tup_add(cur, (0, -1))
        left_pos_char = map_at(map, left_pos)

        if left_pos_char == '#':
            return False
        elif left_pos_char == ']':
            if not move_left(get_nbr_prev(left_pos), 2):
                return False
        elif left_pos_char != '.':
            raise ValueError("Unexpected char")

        if not map_at(map, left_pos) == '.':
            raise ValueError("Unexpected obstacle")

        if width == 1:
            map_set(map, left_pos, map_at(map, cur))
            map_set(map, cur, '.')
        elif width == 2:
            nbr = get_nbr(cur)
            map_set(map, left_pos, map_at(map, cur))
            map_set(map, cur, map_at(map, nbr))
            map_set(map, nbr, '.')
        else:
            raise ValueError("Unexpected width")
        return True

    def move_right(cur: Tuple[int, int], width: int) -> bool:
        if width == 1:
            right_pos = tup_add(cur, (0, 1))
            right_pos_char = map_at(map, right_pos)

            if right_pos_char == '#': return False
            elif right_pos_char == '[':
                if not move_right(right_pos, 2): return False
            elif right_pos_char != '.':
                raise ValueError("Unexpected char")

            if not map_at(map, right_pos) == '.':
                raise ValueError("Unexpected obstacle")

            map_set(map, right_pos, map_at(map, cur))
            map_set(map, cur, '.')
        elif width == 2:
            right_pos = tup_add(cur, (0, 2))
            right_pos_char = map_at(map, right_pos)

            if right_pos_char == '#': return False
            elif right_pos_char == '[':
                if not move_right(right_pos, 2): return False
            elif right_pos_char != '.':
                raise ValueError("Unexpected char")

            if not map_at(map, right_pos) == '.':
                raise ValueError("Unexpected obstacle")

            nbr = get_nbr(cur)
            map_set(map, right_pos, map_at(map, nbr))
            map_set(map, nbr, map_at(map, cur))
            map_set(map, cur, '.')
        else:
            raise ValueError("Unexpected width")
        return True

    if map_at(map, cur_pos) != '@':
        raise ValueError("Invalid cur_pos")

    if move == 'v':
        dir = 1, 0
        map, moved = move_vert(dir, map, cur_pos, 1)
        return map, tup_add(cur_pos, dir) if moved else cur_pos
    elif move == '^':
        dir = -1, 0
        map, moved = move_vert(dir, map, cur_pos, 1)
        return map, tup_add(cur_pos, dir) if moved else cur_pos
    elif move == '<':
        dir = 0, -1
        moved = move_left(cur_pos, 1)
        return map, tup_add(cur_pos, dir) if moved else cur_pos
    elif move == '>':
        dir = 0, 1
        moved = move_right(cur_pos, 1)
        return map, tup_add(cur_pos, dir) if moved else cur_pos
    else:
        raise ValueError("Invalid move")


def partTwo(contents: str):
    [map_str, moves_str] = contents.split("\n\n")
    map = [list(s) for s in resize(map_str).split("\n")]
    moves = list(moves_str.replace("\n", ""))

    cur_pos = None
    for r, row in enumerate(map):
        for c, char in enumerate(row):
            if char == '@':
                if cur_pos is not None:
                    raise ValueError("Two start positions")
                else:
                    cur_pos = r, c
    if cur_pos is None:
        raise ValueError("No start position")

    for move in moves:
        map, cur_pos = make_move_2(move, map, cur_pos)

    print("\n".join(["".join(row) for row in map]))

    total = 0
    for r, row in enumerate(map):
        for c, char in enumerate(row):
            if char == '[':
                total += 100 * r + c
    print(total)


if __name__ == "__main__":
    args = sys.argv[1:]
    if not args:
        print("Expected filenmae", file=sys.stderr)
        exit(1)

    filename = args[0]
    if len(args) > 1:
        part = int(args[1])
    else:
        part = 1

    with open(filename, 'r') as f:
        contents = f.read()

    if part == 1:
        partOne(contents)
    elif part == 2:
        partTwo(contents)
    else:
        print("Invalid part", file=sys.stderr)
        exit(1)
