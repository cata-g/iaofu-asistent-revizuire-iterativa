def next_greater(nums: list[int]) -> list[int]:
    result = [-1] * len(nums)
    stack = []
    for i, x in enumerate(nums):
        while stack and nums[stack[-1]] <= x:
            result[stack.pop()] = x
        stack.append(i)
    return result
