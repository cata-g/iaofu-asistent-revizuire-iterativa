def min_meeting_rooms(intervals: list[list[int]]) -> int:
    starts = sorted(s for s, _ in intervals)
    ends = sorted(e for _, e in intervals)
    rooms = 0
    j = 0
    for s in starts:
        if ends[j] <= s:
            j += 1
        else:
            rooms += 1
    return rooms
