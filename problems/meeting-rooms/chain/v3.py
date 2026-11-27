import heapq


def min_meeting_rooms(intervals: list[list[int]]) -> int:
    intervals.sort()
    ends = []
    for start, end in intervals:
        if ends and ends[0] < start:
            heapq.heappop(ends)
        heapq.heappush(ends, end)
    return len(ends)
