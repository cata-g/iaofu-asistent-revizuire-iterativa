def two_sum(nums: list[int], target: int) -> list[int]:
    seen = {}
    for i, x in enumerate(nums):
        seen[x] = i
        if target - x in seen:
            return [seen[target - x], i]
    return []
