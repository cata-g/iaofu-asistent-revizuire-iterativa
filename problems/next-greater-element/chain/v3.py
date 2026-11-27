def next_greater(nums: list[int]) -> list[int]:
    result = [-1] * len(nums)
    stack = []
    for i in range(len(nums) - 1, -1, -1):
        if stack and stack[-1] <= nums[i]:
            stack.pop()
        if stack:
            result[i] = stack[-1]
        stack.append(nums[i])
    return result
