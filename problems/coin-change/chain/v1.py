def coin_change(coins: list[int], amount: int) -> int:
    count = 0
    for coin in sorted(coins, reverse=True):
        count += amount // coin
        amount %= coin
    return count if amount == 0 else -1
