def daily_temperatures(temperatures: list[int]) -> list[int]:
    answer = [0] * len(temperatures)
    waiting = []
    for day, t in enumerate(temperatures):
        while waiting and temperatures[waiting[-1]] < t:
            previous = waiting.pop()
            answer[previous] = day - previous
        waiting.append(day)
    return answer
