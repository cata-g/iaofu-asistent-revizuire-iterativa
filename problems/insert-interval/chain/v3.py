def insert(intervals: list[list[int]], new_interval: list[int]) -> list[list[int]]:
    start, end = new_interval
    result = []
    placed = False
    for current in intervals:
        if current[1] < start:
            result.append(current)
        elif current[0] > end:
            if not placed:
                result.append([start, end])
                placed = True
            result.append(current)
        else:
            start = min(start, current[0])
            end = max(end, current[1])
    return result
