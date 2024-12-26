import sys
from typing import Generator, TypeVar

T = TypeVar("T")
U = TypeVar("U")


class Lock:
    def __init__(self, obj: list[str]):
        if not is_lock(obj):
            raise ValueError("Not a lock")

        tumbler_rows = obj[1:]
        self.max_height = len(tumbler_rows) - 1
        self.heights = [-1] * len(obj[0])

        for r, row in enumerate(tumbler_rows):
            for c, char in enumerate(row):
                if char == '.' and self.heights[c] == -1:
                    self.heights[c] = r

        if any([h == -1 for h in self.heights]):
            raise ValueError("Unexpected missing value")

    def fits_key(self, key: "Key") -> bool:
        """
        Returns true if the key fits in the lock. It does _not_ return true
        if the key is a _perfect_ match, only whether there is no overlap of
        teeth and tumblers.
        """
        if self.max_height != key.max_height:
            raise ValueError("Heights don't match")
        if len(self.heights) != len(key.heights):
            raise ValueError("Widths don't match")

        sums = [lh + kh for lh, kh in zip(self.heights, key.heights)]
        return all([s <= self.max_height for s in sums])


class Key:
    def __init__(self, obj: list[str]):
        if not is_key(obj):
            raise ValueError("Not a key")

        teeth_rows = obj[-2::-1]
        self.max_height = len(teeth_rows) - 1
        self.heights = [-1] * len(obj[-1])

        for r, row in enumerate(teeth_rows):
            for c, char in enumerate(row):
                if char == '.' and self.heights[c] == -1:
                    self.heights[c] = r

        if any([h == -1 for h in self.heights]):
            raise ValueError("Unexpected missing value")


def is_lock(obj: list[str]) -> bool:
    return all([c == '#' for c in obj[0]]) and all([c == '.' for c in obj[-1]])


def is_key(obj: list[str]) -> bool:
    return all([c == '.' for c in obj[0]]) and all([c == '#' for c in obj[-1]])


def cross_product(l1: list[T], l2: list[U]) -> Generator[tuple[T, U], None, None]:
    if l1 and l2:
        for item in l2:
            yield l1[0], item
        yield from cross_product(l1[1:], l2)


def partOne(objs: list[list[str]]) -> int:
    locks = [Lock(l) for l in objs if is_lock(l)]
    keys = [Key(k) for k in objs if is_key(k)]

    count = 0
    for l, k in cross_product(locks, keys):
        if l.fits_key(k):
            count += 1

    return count


def partTwo(objs: list[list[str]]) -> int:
    _ = objs
    return 50  # there was no puzzle :)


if __name__ == '__main__':
    args = sys.argv[1:]

    if not args:
        raise AssertionError("Expected filename")
    [filename, *args] = args

    if args:
        part = int(args[0])
    else:
        part = 1

    with open(filename, 'r') as f:
        objs = f.read().strip().split('\n\n')
        objs = [obj.strip().splitlines() for obj in objs]

    if part == 1:
        print(partOne(objs))
    elif part == 2:
        print(partTwo(objs))
    else:
        raise ValueError("Invalid part")
