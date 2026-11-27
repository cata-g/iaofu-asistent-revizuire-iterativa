from collections import deque


def can_finish(num_courses: int, prerequisites: list[list[int]]) -> bool:
    after = [[] for _ in range(num_courses)]
    waiting = [0] * num_courses
    for course, before in prerequisites:
        after[before].append(course)
        waiting[course] += 1
    ready = deque(c for c in range(num_courses) if waiting[c] == 0)
    taken = 0
    while ready:
        current = ready.popleft()
        taken += 1
        for nxt in after[current]:
            waiting[nxt] -= 1
            if waiting[nxt] == 0:
                ready.append(nxt)
    return taken == num_courses
