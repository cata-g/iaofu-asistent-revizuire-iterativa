def min_subarray_len(target: int, nums: list[int]) -> int:
    best = len(nums) + 1
    total = 0
    left = 0
    for right in range(len(nums)):
        total += nums[right]
        while total >= target:
            best = min(best, right - left + 1)
            total -= nums[left]
            left += 1
    return best
