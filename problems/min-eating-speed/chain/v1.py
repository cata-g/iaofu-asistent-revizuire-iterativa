def min_eating_speed(piles: list[int], h: int) -> int:
    k = 1
    while True:
        hours = 0
        for p in piles:
            hours += (p + k - 1) // k
        if hours <= h:
            return k
        k += 1
