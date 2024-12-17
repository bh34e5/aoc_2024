import sys
from typing import Dict, List, Optional, Tuple, Union


def renumber(chars: List[List[str]], h: int, w: int) -> List[List[int]]:
    res = [[-1 for _ in range(w)] for _ in range(h)]
    def valid(p):
        r, c = p
        return 0 <= r < h and 0 <= c < w

    def process(row: int, col: int, target: str, mapped: int):
        if res[row][col] != -1: return        # already processed
        if chars[row][col] != target: return  # not target

        res[row][col] = mapped

        up = row - 1, col
        dn = row + 1, col
        lt = row, col - 1
        rt = row, col + 1

        if valid(up): process(*up, target, mapped)
        if valid(dn): process(*dn, target, mapped)
        if valid(lt): process(*lt, target, mapped)
        if valid(rt): process(*rt, target, mapped)

    cur = 0
    for row in range(h):
        for col in range(w):
            if res[row][col] == -1:
                target = chars[row][col]
                process(row, col, target, cur)

                cur += 1

    for row in res:
        for num in row:
            if num == -1: raise ValueError("Unmatched number!")
    return res


def vert_diffs(chars: List[List[int]]) -> Dict[Union[int, None], int]:
    w = len(chars[0])
    nones = [None] * w

    res: Dict[Union[int, None], int] = {}
    for r_one, r_two in zip([nones, *chars], [*chars, nones]):
        for c_one, c_two in zip(r_one, r_two):
            if c_one != c_two:
                res[c_one] = res.get(c_one, 0) + 1
                res[c_two] = res.get(c_two, 0) + 1
    return res


def hori_diffs(chars: List[List[int]]) -> Dict[Union[int, None], int]:
    res: Dict[Union[int, None], int] = {}
    for row in chars:
        for c_one, c_two in zip([None, *row], [*row, None]):
            if c_one != c_two:
                res[c_one] = res.get(c_one, 0) + 1
                res[c_two] = res.get(c_two, 0) + 1
    return res

def merge_diffs(d_one: Dict[Union[int, None], int],
                d_two: Dict[Union[int, None], int]
                ) -> Dict[Union[int, None], int]:
    for key, val in d_two.items():
        d_one[key] = d_one.get(key, 0) + val
    return d_one


def part_one(lines: List[str]) -> int:
    chars = [[*line.strip()] for line in lines]
    h = len(chars)
    w = len(chars[0])
    if not all([w == len(l) for l in chars]): raise ValueError()

    nums = renumber(chars, h, w)

    area: Dict[Union[int, None], int] = {}
    for row in nums:
        for c in row:
            area[c] = area.get(c, 0) + 1

    fence: Dict[Union[int, None], int] = {}
    merge_diffs(fence, vert_diffs(nums))
    merge_diffs(fence, hori_diffs(nums))

    res = 0
    for c in area:
        if c is not None: res += area[c] * fence[c]

    return res


def get_next_dx(dh, dw):
    if dh == 1 and dw == 0:
        return 0, 1
    elif dh == 0 and dw == 1:
        return -1, 0
    elif dh == -1 and dw == 0:
        return 0, -1
    elif dh == 0 and dw == -1:
        return 1, 0
    else:
        raise ValueError("invalid cur dx")

def get_fence(nums: List[List[int]], h: int, w: int) -> Dict[int, int]:
    _ = h
    nones = [None] * w
    expanded = [[(tl, tr, bl, br)
                 for (tl, tr), (bl, br) in zip(zip([None, *top], [*top, None]),
                                               zip([None, *bot], [*bot, None]))]
                for top, bot in zip([nones, *nums], [*nums, nones])]

    res: Dict[int, int] = {}
    def process(t: Tuple[Optional[int], Optional[int], Optional[int], Optional[int]]):
        tl, tr, bl, br = t
        counts: Dict[int, int] = {}
        if tl is not None: counts[tl] = counts.get(tl, 0) + 1
        if tr is not None: counts[tr] = counts.get(tr, 0) + 1
        if bl is not None: counts[bl] = counts.get(bl, 0) + 1
        if br is not None: counts[br] = counts.get(br, 0) + 1

        for k, v in counts.items():
            if v == 1 or v == 3:
                res[k] = res.get(k, 0) + 1
            if v == 2:
                # check diagonals
                if (tl == k and br == k) or (tr == k and bl == k):
                    res[k] = res.get(k, 0) + 2  # two edges

    for r in expanded:
        for v in r:
            process(v)
    return res



def part_two(lines: List[str]) -> int:
    chars = [[*line.strip()] for line in lines]
    h = len(chars)
    w = len(chars[0])
    if not all([w == len(l) for l in chars]): raise ValueError()

    nums = renumber(chars, h, w)

    area: Dict[Union[int, None], int] = {}
    for row in nums:
        for c in row:
            area[c] = area.get(c, 0) + 1

    fence = get_fence(nums, h, w)

    res = 0
    for c in area:
        if c is not None: res += area[c] * fence[c]

    return res


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
