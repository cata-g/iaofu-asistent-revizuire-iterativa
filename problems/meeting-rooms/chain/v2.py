def min_meeting_rooms(intervals: list[list[int]]) -> int:
    intervals.sort()
    rooms = 1
    for i in range(1, len(intervals)):
        if intervals[i][0] < intervals[i - 1][1]:
            rooms += 1
    return rooms
