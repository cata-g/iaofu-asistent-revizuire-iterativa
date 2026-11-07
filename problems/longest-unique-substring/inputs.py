def generate(n, rng):
    """Mostly distinct characters with a few repeats, so the answer is long and brute force is quadratic."""
    alphabet = [chr(0x4E00 + i) for i in range(n)]
    rng.shuffle(alphabet)
    chars = alphabet[:n]
    for _ in range(3):
        i, j = rng.randrange(n), rng.randrange(n)
        chars[i] = chars[j]
    return {"s": "".join(chars)}


def is_valid(args):
    s = args.get("s")
    return isinstance(s, str) and len(s) <= 10**5
