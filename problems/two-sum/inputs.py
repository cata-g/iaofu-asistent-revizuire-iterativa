from collections import Counter


def generate(n, rng):
    """n-1 distinct even values and one odd value; the odd target has exactly one pair.

    The pair is placed at the end so a quadratic solution has to scan everything.
    """
    evens = [2 * v for v in rng.sample(range(-5 * 10**8, 5 * 10**8), n - 1)]
    odd = 2 * rng.randrange(-5 * 10**8, 5 * 10**8 - 1) + 1
    nums = evens[:-1] + [odd, evens[-1]]
    return {"nums": nums, "target": odd + evens[-1]}


def is_valid(args):
    """Checks the constraints, including that exactly one pair sums to target."""
    nums, target = args.get("nums"), args.get("target")
    if not isinstance(nums, list) or not isinstance(target, int) or isinstance(target, bool):
        return False
    if not 2 <= len(nums) <= 10**5:
        return False
    if not all(isinstance(x, int) and not isinstance(x, bool) and -10**9 <= x <= 10**9 for x in nums):
        return False
    counts = Counter(nums)
    pairs = 0
    for x, c in counts.items():
        y = target - x
        if y == x:
            pairs += c * (c - 1) // 2
        elif x < y:
            pairs += c * counts.get(y, 0)
    return pairs == 1
