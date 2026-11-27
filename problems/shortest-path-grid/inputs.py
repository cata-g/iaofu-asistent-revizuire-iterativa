from math import isqrt


def _reachable(grid):
    rows, cols = len(grid), len(grid[0])
    seen = {(0, 0)}
    stack = [(0, 0)]
    while stack:
        r, c = stack.pop()
        if (r, c) == (rows - 1, cols - 1):
            return True
        for nr, nc in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
            if 0 <= nr < rows and 0 <= nc < cols and grid[nr][nc] == "." and (nr, nc) not in seen:
                seen.add((nr, nc))
                stack.append((nr, nc))
    return False


def generate(n, rng):
    """Roughly square grid with n cells and ~30% random walls, redrawn until the corners connect.

    The walls force detours, so the shortest path is longer than the Manhattan distance
    and the number of equally short paths grows quickly with n.
    """
    rows = max(1, isqrt(n))
    cols = n // rows
    while True:
        cells = [["#" if rng.random() < 0.3 else "." for _ in range(cols)] for _ in range(rows)]
        cells[0][0] = cells[rows - 1][cols - 1] = "."
        grid = ["".join(row) for row in cells]
        if _reachable(grid):
            return {"grid": grid}


def is_valid(args):
    grid = args.get("grid")
    if not isinstance(grid, list) or not grid:
        return False
    if not all(isinstance(row, str) for row in grid):
        return False
    cols = len(grid[0])
    if cols == 0 or any(len(row) != cols for row in grid):
        return False
    if len(grid) * cols > 10**5:
        return False
    return all(ch in ".#" for row in grid for ch in row)
