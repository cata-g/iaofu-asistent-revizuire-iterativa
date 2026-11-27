def coin_change(coins: list[int], amount: int) -> int:
    if amount == 0:
        return 0
    best = -1
    for coin in coins:
        if coin <= amount:
            rest = coin_change(coins, amount - coin)
            if rest != -1 and (best == -1 or rest + 1 < best):
                best = rest + 1
    return best
