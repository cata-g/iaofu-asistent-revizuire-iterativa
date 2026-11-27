def min_subarray_len(target: int, nums: list[int]) -> int:
    best = 0
    for i in range(len(nums)):
        total = 0
        for j in range(i, len(nums)):
            total += nums[j]
            if total >= target and (best == 0 or j - i + 1 < best):
                best = j - i + 1
    return best
