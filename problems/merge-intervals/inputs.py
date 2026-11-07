def generate(n, rng):
    """Random short intervals on a line sized so roughly half of them overlap a neighbour."""
    span = 20 * n
    intervals = []
    for _ in range(n):
        start = rng.randrange(span)
        intervals.append([start, start + rng.randrange(1, 25)])
    return {"intervals": intervals}


def is_valid(args):
    intervals = args.get("intervals")
    if not isinstance(intervals, list) or not 1 <= len(intervals) <= 10**5:
        return False
    for item in intervals:
        if not isinstance(item, list) or len(item) != 2:
            return False
        start, end = item
        if not all(isinstance(x, int) and not isinstance(x, bool) for x in item):
            return False
        if not 0 <= start <= end <= 10**9:
            return False
    return True
