def generate(n, rng):
    """n separated intervals; the new one touches an interval at each end and covers ~70% of them.

    Its start equals the end of an interval at ~30% and its end equals the start of one at ~98%,
    so strict comparisons fail, and the long run of merges makes restarted scans quadratic.
    """
    intervals = []
    pos = 0
    for _ in range(n):
        start = pos + rng.randrange(1, 6)
        end = start + rng.randrange(1, 6)
        intervals.append([start, end])
        pos = end
    first = n * 3 // 10
    last = n - n // 50 - 1
    return {"intervals": intervals, "new_interval": [intervals[first][1], intervals[last][0]]}


def _is_int(x):
    return isinstance(x, int) and not isinstance(x, bool)


def _is_interval(item):
    return (isinstance(item, list) and len(item) == 2 and all(_is_int(x) for x in item)
            and 0 <= item[0] <= item[1] <= 10**9)


def is_valid(args):
    if not isinstance(args, dict):
        return False
    intervals, new_interval = args.get("intervals"), args.get("new_interval")
    if not isinstance(intervals, list) or not 0 <= len(intervals) <= 10**5:
        return False
    if not _is_interval(new_interval) or not all(_is_interval(item) for item in intervals):
        return False
    return all(intervals[i][1] < intervals[i + 1][0] for i in range(len(intervals) - 1))
