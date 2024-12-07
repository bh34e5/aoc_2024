from enum import Enum
import sys
from typing import Literal

OBSTACLE = "obstacle"


class Direction(Enum):
    UP = "up"
    RIGHT = "right"
    DOWN = "down"
    LEFT = "left"

    def next(self) -> 'Direction':
        match self:
            case Direction.UP: return Direction.RIGHT
            case Direction.RIGHT: return Direction.DOWN
            case Direction.DOWN: return Direction.LEFT
            case Direction.LEFT: return Direction.UP

    def pos_in(self, pos: tuple[int, int]) -> tuple[int, int]:
        r, c = pos
        match self:
            case Direction.UP: return r-1, c
            case Direction.RIGHT: return r, c+1
            case Direction.DOWN: return r+1, c
            case Direction.LEFT: return r, c-1


def cell_type(c: str) -> Direction | Literal["obstacle"] | None:
    match c:
        case '.': return None
        case '#': return OBSTACLE
        case '^': return Direction.UP
        case '>': return Direction.RIGHT
        case 'v': return Direction.DOWN
        case '<': return Direction.LEFT
        case _: raise ValueError("Unexpected cell")


class Map:
    def __init__(self, lines: list[str]):
        self.height = len(lines)
        self.width = len(lines[0].strip())

        self.map: dict[int, dict[int, Direction | Literal["obstacle"] | None]]
        self.map = {row: {col: cell_type(c) for col, c in enumerate(line.strip())}
                    for row, line in enumerate(lines)}

        self.positions: set[tuple[int, int]]
        self.positions = set()

        self.pos_dirs: set[tuple[tuple[int, int], Direction]]
        self.pos_dirs = set()

        self.loopables: set[tuple[int, int]]
        self.loopables = set()

        for r, row in self.map.items():
            for c, col in row.items():
                if isinstance(col, Direction):
                    self.map[r][c] = None
                    self.position = r, c
                    self.direction = col
                    self.start = self.position
                    self.positions.add(self.position)
                    self.pos_dirs.add((self.position, self.direction))

    def at(self, pos: tuple[int, int]):
        return self.map[pos[0]][pos[1]]

    def valid(self, pos: tuple[int, int]) -> bool:
        return 0 <= pos[0] < self.height and 0 <= pos[1] < self.width

    def occupied(self, pos: tuple[int, int]) -> bool:
        if not self.valid(pos):
            raise ValueError("Invalid pos")
        return self.at(pos) == OBSTACLE

    def step(self) -> bool:
        next_pos = self.direction.pos_in(self.position)
        if next_pos != self.start and self.loopable(next_pos):
            self.loopables.add(next_pos)

        if self.valid(next_pos):
            # on the map
            if self.occupied(next_pos):
                # don't move, but rotate
                self.direction = self.direction.next()
                self.pos_dirs.add((self.position, self.direction))
            else:
                # move forward and track the new position
                self.position = next_pos
                self.positions.add(self.position)
                self.pos_dirs.add((self.position, self.direction))
            return True
        else:
            # off the map
            return False

    def loopable(self, next_pos: tuple[int, int]) -> bool:
        if not self.valid(next_pos) \
                or self.occupied(next_pos) \
                or next_pos in self.positions:
            return False

        block_pos = next_pos
        pos_dirs = self.pos_dirs.copy()
        cur_pos = self.position
        cur_dir = self.direction
        while True:
            next_pos = cur_dir.pos_in(cur_pos)
            if not self.valid(next_pos):
                # went off the map, didn't loop
                return False
            elif next_pos == block_pos or self.occupied(next_pos):
                cur_dir = cur_dir.next()
            else:
                cur_pos = next_pos

            if (cur_pos, cur_dir) in pos_dirs:
                # we have been in this configuration before
                return True
            else:
                pos_dirs.add((cur_pos, cur_dir))


def part_one(lines: list[str]) -> int:
    m = Map(lines)
    while m.step():
        pass

    return len(m.positions)


def part_two(lines: list[str]):
    m = Map(lines)
    while m.step():
        pass

    return len(m.loopables)


if __name__ == '__main__':
    args = sys.argv[1:]
    filename = args[0]
    with open(filename, 'r') as f:
        lines = f.readlines()

    part = 1 if len(args) < 2 else int(args[1])
    if part == 1:
        print(part_one(lines))
    elif part == 2:
        print(part_two(lines))
    else:
        raise ValueError("Invalid part", part)
