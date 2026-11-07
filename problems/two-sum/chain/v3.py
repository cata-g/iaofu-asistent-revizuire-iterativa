def two_sum(nums: list[int], target: int) -> list[int]:
    values = sorted(nums)
    left, right = 0, len(values) - 1
    while left < right:
        total = values[left] + values[right]
        if total == target:
            return [left, right]
        if total < target:
            left += 1
        else:
            right -= 1
    return []
