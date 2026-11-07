def generate(n, rng):
    """A balanced string of length n: a deep nest followed by random flat pairs.

    The deep nest makes repeated replace("()", "") quadratic.
    """
    opens = "([{"
    closes = {"(": ")", "[": "]", "{": "}"}
    depth = n // 4
    nest = [rng.choice(opens) for _ in range(depth)]
    s = nest + [closes[c] for c in reversed(nest)]
    while len(s) < n:
        c = rng.choice(opens)
        s += [c, closes[c]]
    return {"s": "".join(s[:n])}


def is_valid(args):
    s = args.get("s")
    return isinstance(s, str) and 1 <= len(s) <= 10**5 and all(c in "()[]{}" for c in s)
