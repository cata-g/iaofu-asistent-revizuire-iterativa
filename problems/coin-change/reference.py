def coin_change(coins: list[int], amount: int) -> int:
    unreachable = amount + 1
    fewest = [0] + [unreachable] * amount
    for coin in coins:
        for value in range(coin, amount + 1):
            candidate = fewest[value - coin] + 1
            if candidate < fewest[value]:
                fewest[value] = candidate
    return fewest[amount] if fewest[amount] <= amount else -1
