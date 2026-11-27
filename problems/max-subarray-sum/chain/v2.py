def max_subarray(nums: list[int]) -> int:
    best = 0
    current = 0
    for x in nums:
        current += x
        if current < 0:
            current = 0
        best = max(best, current)
    return best
