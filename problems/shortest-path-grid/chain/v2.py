from collections import deque


def shortest_path(grid: list[str]) -> int:
    rows, cols = len(grid), len(grid[0])
    visited = set()
    queue = deque([(0, 0, 0)])
    while queue:
        r, c, d = queue.popleft()
        visited.add((r, c))
        if r == rows - 1 and c == cols - 1:
            return d
        for nr, nc in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
            if 0 <= nr < rows and 0 <= nc < cols and grid[nr][nc] == "." and (nr, nc) not in visited:
                queue.append((nr, nc, d + 1))
    return -1
