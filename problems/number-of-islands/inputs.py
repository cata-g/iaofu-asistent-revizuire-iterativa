from math import isqrt


def generate(n, rng):
    """Roughly square grid with n cells: a snake-shaped island on top, random small islands below.

    The snake is a one-cell-wide path covering about a quarter of the cells, so a recursive
    traversal goes as deep as the snake is long (more than 1000 calls from n = 10^4).
    The bottom half has ~40% land, which gives many small islands of irregular shape.
    """
    rows = max(1, isqrt(n))
    cols = n // rows
    cells = [["0"] * cols for _ in range(rows)]
    half = rows // 2
    for r in range(half):
        if r % 2 == 0:
            cells[r] = ["1"] * cols
        else:
            cells[r][cols - 1 if (r // 2) % 2 == 0 else 0] = "1"
    for r in range(half + 1, rows):
        for c in range(cols):
            if rng.random() < 0.4:
                cells[r][c] = "1"
    return {"grid": ["".join(row) for row in cells]}


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
    return all(ch in "01" for row in grid for ch in row)
