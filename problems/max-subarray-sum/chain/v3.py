def max_subarray(nums: list[int]) -> int:
    prefix = [0]
    for x in nums:
        prefix.append(prefix[-1] + x)
    return max(prefix) - min(prefix)
