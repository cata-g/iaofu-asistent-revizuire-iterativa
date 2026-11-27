def generate(n, rng):
    """Short meetings on a timeline sized so a handful overlap at any moment.

    Endpoints are small integers, so many meetings end exactly when another starts.
    """
    span = 3 * n
    intervals = []
    for _ in range(n):
        start = rng.randrange(span)
        intervals.append([start, start + rng.randint(1, 30)])
    return {"intervals": intervals}


def is_valid(args):
    intervals = args.get("intervals")
    if not isinstance(intervals, list) or not 1 <= len(intervals) <= 10**5:
        return False
    for item in intervals:
        if not isinstance(item, list) or len(item) != 2:
            return False
        if not all(isinstance(x, int) and not isinstance(x, bool) for x in item):
            return False
        start, end = item
        if not 0 <= start < end <= 10**9:
            return False
    return True
