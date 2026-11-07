def first_positions(nums: list[int], queries: list[int]) -> list[int]:
    answers = []
    for target in queries:
        lo, hi = 0, len(nums)
        while lo < hi:
            mid = (lo + hi) // 2
            if nums[mid] < target:
                lo = mid + 1
            else:
                hi = mid
        answers.append(lo)
    return answers
