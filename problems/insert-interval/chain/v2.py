def insert(intervals: list[list[int]], new_interval: list[int]) -> list[list[int]]:
    start, end = new_interval
    result = []
    i = 0
    while i < len(intervals) and intervals[i][1] < start:
        result.append(intervals[i])
        i += 1
    while i < len(intervals) and intervals[i][0] <= end:
        end = max(end, intervals[i][1])
        i += 1
    result.append([start, end])
    while i < len(intervals):
        result.append(intervals[i])
        i += 1
    return result
