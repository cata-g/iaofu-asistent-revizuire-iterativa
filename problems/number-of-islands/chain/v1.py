def num_islands(grid: list[str]) -> int:
    rows, cols = len(grid), len(grid[0])
    count = 0
    for r in range(rows):
        for c in range(cols):
            if grid[r][c] != "1":
                continue
            up = r > 0 and grid[r - 1][c] == "1"
            left = c > 0 and grid[r][c - 1] == "1"
            if not up and not left:
                count += 1
    return count
