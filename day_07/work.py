import math
import sys


class PotentialOp:
    ops: dict[int, list["PotentialOp"]] = {}
    args: dict[int, list[list[int]]] = {}

    def __init__(self, target: int, op_list: list[int]):
        if target < 0:
            raise ValueError("Invalid target")
        if not op_list:
            raise ValueError("Invaldi op_list")

        PotentialOp.ops.get(target, []).append(self)
        PotentialOp.args.get(target, []).append(op_list)
        self.result: bool | None = None
        self.target = target
        self.op_list = op_list

    def evaluate(self, /, with_cat=False):
        # already evaluated, use that result
        if self.result is not None:
            return self.result

        # down to the last entry, check if it matches target
        if len(self.op_list) == 1:
            valid = self.target == self.op_list[0]
            self.result = valid
            return valid

        [*first, last] = self.op_list
        if with_cat:
            pow_ten = math.floor(math.log10(last) + 1)
            ten_pow = 10**pow_ten
            mod = self.target - last
            if mod >= 0 and mod % ten_pow == 0:
                concat_op = PotentialOp.for_ops(mod // ten_pow, first)
                concat_op_res = concat_op.evaluate(with_cat=with_cat)
                if concat_op_res:
                    self.result = concat_op_res
                    return concat_op_res

        if self.target % last == 0:
            # try multplying
            mul_op = PotentialOp.for_ops(self.target // last, first)
            mul_op_res = mul_op.evaluate(with_cat=with_cat)
            if mul_op_res:
                self.result = mul_op_res
                return mul_op_res

        if self.target - last > 0:
            add_op = PotentialOp.for_ops(self.target - last, first)
            add_op_res = add_op.evaluate(with_cat=with_cat)
            self.result = add_op_res
            return add_op_res

        self.result = False
        return False

    @classmethod
    def for_ops(cls, target: int, op_list: list[int]) -> "PotentialOp":
        if target in cls.ops:
            for t_p_op, t_op_list in zip(cls.ops[target], cls.args[target]):
                if t_op_list == op_list:
                    return t_p_op
        return cls(target, op_list)


def part_one(lines: list[str]) -> int:
    ops = [PotentialOp(target, op_list)
           for target, op_list in [(int(t_str),
                                    [int(o_str)
                                     for o_str in r_str.strip().split(" ")])
                                   for t_str, r_str in [line.split(":")
                                                        for line in lines]]]

    res_list = [op.target for op in ops if op.evaluate()]
    return sum(res_list)


def part_two(lines: list[str]) -> int:
    ops = [PotentialOp(target, op_list)
           for target, op_list in [(int(t_str),
                                    [int(o_str)
                                     for o_str in r_str.strip().split(" ")])
                                   for t_str, r_str in [line.split(":")
                                                        for line in lines]]]

    res_list = [op.target for op in ops if op.evaluate(with_cat=True)]
    return sum(res_list)


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
