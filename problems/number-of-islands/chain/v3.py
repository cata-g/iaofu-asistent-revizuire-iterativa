def num_islands(grid: list[str]) -> int:
    rows, cols = len(grid), len(grid[0])
    seen = set()
    count = 0
    for i in range(rows):
        for j in range(cols):
            if grid[i][j] != "1" or (i, j) in seen:
                continue
            count += 1
            seen.add((i, j))
            stack = [(i, j)]
            while stack:
                r, c = stack.pop()
                for nr, nc in ((r + 1, c), (r - 1, c), (r, c + 1), (r, c - 1)):
                    if nr < rows and nc < cols and grid[nr][nc] == "1" and (nr, nc) not in seen:
                        seen.add((nr, nc))
                        stack.append((nr, nc))
    return count
