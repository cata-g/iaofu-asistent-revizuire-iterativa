def group_anagrams(strs: list[str]) -> list[list[str]]:
    groups = []
    for s in strs:
        placed = False
        for group in groups:
            if sorted(group[0]) == sorted(s):
                group.append(s)
                placed = True
                break
        if not placed:
            groups.append([s])
    return groups
