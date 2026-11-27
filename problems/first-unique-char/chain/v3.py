def first_uniq_char(s: str) -> int:
    candidates = {}
    for i, c in enumerate(s):
        if c in candidates:
            del candidates[c]
        else:
            candidates[c] = i
    if not candidates:
        return -1
    return min(candidates.values())
