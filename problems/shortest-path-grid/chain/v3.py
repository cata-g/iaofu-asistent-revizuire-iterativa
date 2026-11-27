from collections import deque


def shortest_path(grid: list[str]) -> int:
    rows, cols = len(grid), len(grid[0])
    if grid[0][0] == "#" or grid[rows - 1][cols - 1] == "#":
        return -1
    visited = {(0, 0)}
    queue = deque([(0, 0, 0)])
    while queue:
        r, c, d = queue.popleft()
        for nr, nc in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
            if 0 <= nr < rows and 0 <= nc < cols and grid[nr][nc] == "." and (nr, nc) not in visited:
                if nr == rows - 1 and nc == cols - 1:
                    return d + 1
                visited.add((nr, nc))
                queue.append((nr, nc, d + 1))
    return -1
