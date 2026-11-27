def first_uniq_char(s: str) -> int:
    counts = {}
    for c in s:
        counts[c] = counts.get(c, 0) + 1
    for i, c in enumerate(s):
        if counts[c] == 1:
            return i
    return -1
