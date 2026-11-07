def first_positions(nums: list[int], queries: list[int]) -> list[int]:
    answers = []
    for target in queries:
        lo, hi = 0, len(nums) - 1
        found = -1
        while lo <= hi:
            mid = (lo + hi) // 2
            if nums[mid] == target:
                found = mid
                break
            if nums[mid] < target:
                lo = mid + 1
            else:
                hi = mid - 1
        answers.append(found)
    return answers
