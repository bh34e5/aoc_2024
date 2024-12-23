import sys
from functools import cache


def test_str(avail: list[str], to_test: str) -> bool:
    if not to_test:
        return True

    for test in avail:
        if to_test.startswith(test):
            if test_str(avail, to_test[len(test):]):
                return True
    return False


@cache
def count_test_str(avail: str, to_test: str) -> int:
    if not to_test:
        return 1

    count = 0
    for test in avail.split(","):
        if to_test.startswith(test):
            count += count_test_str(avail, to_test[len(test):])
    return count


def partOne(avail: list[str], patterns: list[str]) -> int:
    return len([test for test in patterns if test_str(avail, test)])


def partTwo(avail: list[str], patterns: list[str]) -> int:
    avail_str = ",".join(avail)
    return sum([count_test_str(avail_str, test) for test in patterns])


if __name__ == "__main__":
    args = sys.argv[1:]

    [filename, *args] = args
    if args:
        part = int(args[0])
    else:
        part = 1

    with open(filename, 'r') as f:
        contents = f.read()
        lines = contents.splitlines()
        [avail, _, *patterns] = lines

        avail = avail.split(", ")

    if part == 1:
        print(partOne(avail, patterns))
    elif part == 2:
        print(partTwo(avail, patterns))
    else:
        raise ValueError("Invalid part")
