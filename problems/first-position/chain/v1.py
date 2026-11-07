def first_positions(nums: list[int], queries: list[int]) -> list[int]:
    answers = []
    for target in queries:
        position = -1
        for i, x in enumerate(nums):
            if x == target:
                position = i
                break
        answers.append(position)
    return answers
