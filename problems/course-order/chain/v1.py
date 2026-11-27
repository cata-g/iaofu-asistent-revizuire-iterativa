def can_finish(num_courses: int, prerequisites: list[list[int]]) -> bool:
    after = [[] for _ in range(num_courses)]
    for course, before in prerequisites:
        after[before].append(course)
    visited = [False] * num_courses
    for start in range(num_courses):
        if visited[start]:
            continue
        visited[start] = True
        stack = [start]
        while stack:
            current = stack.pop()
            for nxt in after[current]:
                if visited[nxt]:
                    return False
                visited[nxt] = True
                stack.append(nxt)
    return True
