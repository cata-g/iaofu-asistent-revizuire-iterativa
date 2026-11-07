def longest_unique(s: str) -> int:
    last = {}
    start = 0
    best = 0
    for i, c in enumerate(s):
        if c in last:
            start = last[c] + 1
        last[c] = i
        best = max(best, i - start + 1)
    return best
