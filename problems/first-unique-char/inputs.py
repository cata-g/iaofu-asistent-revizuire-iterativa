def generate(n, rng):
    """Every letter but one appears at least twice; the single unique letter sits near the end.

    Letters come in blocks of 2 or 3, so their counts are odd as often as even. A solution that
    rescans the string for every position has to go through almost all of it.
    """
    letters = "abcdefghijklmnopqrstuvwxyz"
    unique = rng.choice(letters)
    others = [c for c in letters if c != unique]
    m = n - 1
    chars = []
    while len(chars) < m:
        chars.extend([rng.choice(others)] * rng.choice([2, 3]))
    chars = chars[:m]
    if m >= 3 and chars.count(chars[-1]) == 1:
        chars[-1] = chars[0]
    rng.shuffle(chars)
    pos = len(chars) - rng.randrange(min(10, len(chars) + 1))
    chars.insert(pos, unique)
    return {"s": "".join(chars)}


def is_valid(args):
    s = args.get("s")
    if not isinstance(s, str) or not 1 <= len(s) <= 10**5:
        return False
    return all("a" <= c <= "z" for c in s)
