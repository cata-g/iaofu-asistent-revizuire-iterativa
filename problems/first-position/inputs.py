def generate(n, rng):
    """n sorted values with many repeats and n queries, half present and half absent (odd values)."""
    nums = sorted(2 * rng.randrange(n // 4 + 1) for _ in range(n))
    queries = [rng.choice(nums) if rng.random() < 0.5 else 2 * rng.randrange(n) + 1 for _ in range(n)]
    return {"nums": nums, "queries": queries}


def is_valid(args):
    nums, queries = args.get("nums"), args.get("queries")
    if not isinstance(nums, list) or not isinstance(queries, list):
        return False
    if not 1 <= len(nums) <= 10**5 or not 1 <= len(queries) <= 10**5:
        return False
    values = nums + queries
    if not all(isinstance(x, int) and not isinstance(x, bool) and -10**9 <= x <= 10**9 for x in values):
        return False
    return all(nums[i] <= nums[i + 1] for i in range(len(nums) - 1))
