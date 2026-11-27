def generate(n, rng):
    """n distinct sorted values rotated at a random point; the target is the largest value.

    The largest value sits just before the rotation point, at the edge of the sorted part,
    where wrong half choices and boundary mistakes lose it.
    """
    values = sorted(rng.sample(range(-10**9, 10**9), n))
    k = rng.randrange(1, n)
    return {"nums": values[k:] + values[:k], "target": values[-1]}


def _is_int(x):
    return isinstance(x, int) and not isinstance(x, bool)


def is_valid(args):
    """Checks types and ranges, distinct values and that nums is a rotation of a sorted list."""
    if not isinstance(args, dict):
        return False
    nums, target = args.get("nums"), args.get("target")
    if not isinstance(nums, list) or not 1 <= len(nums) <= 10**5 or not _is_int(target):
        return False
    if not -10**9 <= target <= 10**9:
        return False
    if not all(_is_int(x) and -10**9 <= x <= 10**9 for x in nums):
        return False
    if len(set(nums)) != len(nums):
        return False
    drops = sum(1 for i in range(len(nums) - 1) if nums[i] > nums[i + 1])
    return drops == 0 or (drops == 1 and nums[-1] < nums[0])
