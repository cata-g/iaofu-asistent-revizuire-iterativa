def coin_change(coins: list[int], amount: int) -> int:
    fewest = [0] + [float("inf")] * amount
    for value in range(1, amount + 1):
        for coin in coins:
            if coin <= value:
                fewest[value] = min(fewest[value], fewest[value - coin] + 1)
    return fewest[amount]
