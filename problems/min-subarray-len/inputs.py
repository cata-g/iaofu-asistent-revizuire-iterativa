def generate(n, rng):
    """Random values and a target of about a third of the total, so the answer is a long window.

    A dense block of large values makes the shortest window clearly shorter than a typical one,
    and starts in the last third never reach the target.
    """
    nums = [rng.randint(1, 10**4) for _ in range(n)]
    block = max(1, n // 20)
    start = rng.randrange(n - block + 1)
    for i in range(start, start + block):
        nums[i] = rng.randint(9 * 10**3, 10**4)
    target = max(1, sum(nums) // 3)
    return {"target": target, "nums": nums}


def is_valid(args):
    target, nums = args.get("target"), args.get("nums")
    if not isinstance(target, int) or isinstance(target, bool) or not 1 <= target <= 10**9:
        return False
    if not isinstance(nums, list) or not 1 <= len(nums) <= 10**5:
        return False
    return all(isinstance(x, int) and not isinstance(x, bool) and 1 <= x <= 10**4 for x in nums)
