import sys


def diff_op(op, l):
    if not l or len(l) == 1:
        return []
    return list(map(lambda t: op(*t), zip(l[:-1], l[1:])))


def add_holes(l):
    return [[*l[:i], *l[i+1:]] for i in range(len(l))]


def is_safe(nums):
    diffs = diff_op(lambda a, b: a - b, nums)
    valid_diffs = all([1 <= abs(d) <= 3 for d in diffs])
    if not valid_diffs: return False
    same_signs = diff_op(lambda a, b: True if a * b > 0 else False, diffs)
    if not all(same_signs): return False
    return True


def part_one(lines):
    safe = 0
    for line in lines:
        nums = list(map(int, line.split(' ')))
        if is_safe(nums): safe += 1
    return safe


def part_two(lines):
    safe = 0
    for line in lines:
        nums = list(map(int, line.split(' ')))
        if is_safe(nums): safe += 1
        else:
            if any(map(is_safe, add_holes(nums))): safe += 1
    return safe


if __name__ == '__main__':
    args = sys.argv[1:]
    filename = args[0]
    with open(filename, 'r') as f:
        lines = f.readlines()

    part = 1 if len(args) < 2 else int(args[1])
    if part == 1: print(part_one(lines))
    elif part == 2: print(part_two(lines))
    else: raise ValueError("Invalid part", part)
