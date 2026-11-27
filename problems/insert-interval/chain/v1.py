def insert(intervals: list[list[int]], new_interval: list[int]) -> list[list[int]]:
    intervals = sorted(intervals + [new_interval])
    i = 0
    while i < len(intervals) - 1:
        if intervals[i + 1][0] <= intervals[i][1]:
            merged = [intervals[i][0], max(intervals[i][1], intervals[i + 1][1])]
            intervals = intervals[:i] + [merged] + intervals[i + 2:]
        else:
            i += 1
    return intervals
