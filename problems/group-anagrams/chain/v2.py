def group_anagrams(strs: list[str]) -> list[list[str]]:
    groups = {}
    for s in strs:
        key = frozenset(s)
        if key not in groups:
            groups[key] = []
        groups[key].append(s)
    return list(groups.values())
