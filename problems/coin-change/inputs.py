def generate(n, rng):
    """amount = n and six distinct coins: one small odd coin (3, 5 or 7) and five in [8, 60].

    There is no coin of value 1, so taking the largest coin first often fails or uses too many
    coins, and the call tree of an unmemoized recursion explodes already at n = 100.
    """
    small = rng.choice([3, 5, 7])
    coins = [small] + rng.sample([c for c in range(8, 61) if c != 2 * small], 5)
    rng.shuffle(coins)
    return {"coins": coins, "amount": n}


def is_valid(args):
    coins, amount = args.get("coins"), args.get("amount")
    if not isinstance(amount, int) or isinstance(amount, bool) or not 0 <= amount <= 10**5:
        return False
    if not isinstance(coins, list) or not 1 <= len(coins) <= 12:
        return False
    if not all(isinstance(c, int) and not isinstance(c, bool) and 1 <= c <= 10**4 for c in coins):
        return False
    return len(set(coins)) == len(coins)
