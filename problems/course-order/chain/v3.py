from collections import deque


def can_finish(num_courses: int, prerequisites: list[list[int]]) -> bool:
    waiting = {}
    after = {}
    for course, before in prerequisites:
        waiting[course] = waiting.get(course, 0) + 1
        waiting.setdefault(before, 0)
        after.setdefault(before, []).append(course)
    ready = deque(c for c, w in waiting.items() if w == 0)
    taken = 0
    while ready:
        current = ready.popleft()
        taken += 1
        for nxt in after.get(current, []):
            waiting[nxt] -= 1
            if waiting[nxt] == 0:
                ready.append(nxt)
    return taken == num_courses
