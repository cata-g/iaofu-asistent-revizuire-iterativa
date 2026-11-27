def generate(n, rng):
    """n piles of 1..n bananas and only 10% more hours than piles.

    The answer is close to the largest pile (about 0.9·n), so trying speeds one by one
    costs about n checks of n piles each.
    """
    piles = [rng.randint(1, n) for _ in range(n)]
    return {"piles": piles, "h": n + n // 10}


def _is_int(x):
    return isinstance(x, int) and not isinstance(x, bool)


def is_valid(args):
    if not isinstance(args, dict):
        return False
    piles, h = args.get("piles"), args.get("h")
    if not isinstance(piles, list) or not 1 <= len(piles) <= 10**5 or not _is_int(h):
        return False
    if not all(_is_int(p) and 1 <= p <= 10**9 for p in piles):
        return False
    return len(piles) <= h <= 10**9
