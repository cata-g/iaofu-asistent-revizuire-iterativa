def generate(n, rng):
    """n values in [-10^4, 10^4], slightly biased towards negatives.

    The bias keeps the best subarray well inside the array, so neither the whole-array sum
    nor the largest single value is the answer.
    """
    return {"nums": [rng.randint(-10**4, 9 * 10**3) for _ in range(n)]}


def is_valid(args):
    nums = args.get("nums")
    if not isinstance(nums, list) or not 1 <= len(nums) <= 10**5:
        return False
    return all(isinstance(x, int) and not isinstance(x, bool) and -10**4 <= x <= 10**4 for x in nums)
