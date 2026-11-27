def group_anagrams(strs: list[str]) -> list[list[str]]:
    groups = {}
    for s in strs:
        count = [0] * 26
        for c in s:
            count[ord(c) - ord("a")] += 1
        key = tuple(count)
        if key not in groups:
            groups[key] = set()
        groups[key].add(s)
    return [list(g) for g in groups.values()]
