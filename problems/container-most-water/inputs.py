def generate(n, rng):
    """Random heights with a few tall lines scattered inside and low lines at both ends.

    The best pair is then somewhere inside, so the answer depends on which side moves.
    """
    height = [rng.randint(0, 10**4) for _ in range(n)]
    for _ in range(max(1, n // 50)):
        height[rng.randrange(n)] = rng.randint(9 * 10**3, 10**4)
    height[0] = rng.randint(0, 100)
    height[-1] = rng.randint(0, 100)
    return {"height": height}


def is_valid(args):
    height = args.get("height")
    if not isinstance(height, list) or not 2 <= len(height) <= 10**5:
        return False
    return all(isinstance(h, int) and not isinstance(h, bool) and 0 <= h <= 10**4 for h in height)
