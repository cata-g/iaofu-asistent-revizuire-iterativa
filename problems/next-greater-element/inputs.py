def generate(n, rng):
    """A non-increasing prefix (80%) of large values, then a random suffix of small values.

    No element of the prefix has a greater element to its right, so a nested scan is
    quadratic. Both parts repeat values, so treating equal as greater gives wrong answers.
    """
    prefix = n * 4 // 5
    top = sorted((rng.randrange(n, 2 * n) for _ in range(prefix)), reverse=True)
    rest = [rng.randrange(-(n // 10) - 1, n // 10 + 1) for _ in range(n - prefix)]
    return {"nums": top + rest}


def is_valid(args):
    if not isinstance(args, dict):
        return False
    nums = args.get("nums")
    if not isinstance(nums, list) or not 1 <= len(nums) <= 10**5:
        return False
    return all(isinstance(x, int) and not isinstance(x, bool) and -10**9 <= x <= 10**9 for x in nums)
