def next_greater(nums: list[int]) -> list[int]:
    result = []
    for i in range(len(nums)):
        found = -1
        for j in range(i + 1, len(nums)):
            if nums[j] > nums[i]:
                found = nums[j]
                break
        result.append(found)
    return result
