def first_uniq_char(s: str) -> int:
    counts = {}
    for c in s:
        counts[c] = counts.get(c, 0) + 1
    for i in range(len(s) - 1):
        if counts[s[i]] == 1:
            return i
    return -1
