def can_finish(num_courses: int, prerequisites: list[list[int]]) -> bool:
    after = [[] for _ in range(num_courses)]
    for course, before in prerequisites:
        after[before].append(course)
    for start in range(num_courses):
        seen = set()
        stack = list(after[start])
        while stack:
            current = stack.pop()
            if current == start:
                return False
            if current in seen:
                continue
            seen.add(current)
            stack.extend(after[current])
    return True
