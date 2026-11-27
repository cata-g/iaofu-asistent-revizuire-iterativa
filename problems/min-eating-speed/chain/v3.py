def min_eating_speed(piles: list[int], h: int) -> int:
    lo, hi = 1, max(piles) - 1
    while lo < hi:
        k = (lo + hi) // 2
        hours = sum((p + k - 1) // k for p in piles)
        if hours <= h:
            hi = k
        else:
            lo = k + 1
    return lo
