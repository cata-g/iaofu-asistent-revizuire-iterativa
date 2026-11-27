def generate(n, rng):
    """n courses: a long chain over 60% of them, extra forward edges (shared prerequisites),
    and isolated courses at the end; for n with an even number of digits (10^3, 10^5) one
    back edge closes a long cycle inside the chain.

    Course ids are shuffled, so the chain order is not the id order. About 0.93·n pairs.
    """
    order = list(range(n))
    rng.shuffle(order)
    k = max(2, n * 6 // 10)
    pairs = {(order[i + 1], order[i]) for i in range(k - 1)}
    for _ in range(n // 3):
        i, j = sorted(rng.sample(range(k), 2))
        pairs.add((order[j], order[i]))
    if len(str(n)) % 2 == 0:
        a = rng.randrange(k // 3)
        b = rng.randrange(2 * k // 3, k)
        pairs.add((order[a], order[b]))
    prerequisites = [list(p) for p in pairs]
    rng.shuffle(prerequisites)
    return {"num_courses": n, "prerequisites": prerequisites}


def is_valid(args):
    n, prerequisites = args.get("num_courses"), args.get("prerequisites")
    if not isinstance(n, int) or isinstance(n, bool) or not 1 <= n <= 10**5:
        return False
    if not isinstance(prerequisites, list) or len(prerequisites) > 10**5:
        return False
    seen = set()
    for pair in prerequisites:
        if not isinstance(pair, list) or len(pair) != 2:
            return False
        if not all(isinstance(x, int) and not isinstance(x, bool) and 0 <= x < n for x in pair):
            return False
        key = tuple(pair)
        if key in seen:
            return False
        seen.add(key)
    return True
