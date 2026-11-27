def generate(n, rng):
    """About n/4 anagram groups; some bases differ from another only by letter counts ("aab" / "abb").

    Many groups make a pairwise comparison quadratic, short bases repeat whole words, and the
    near-anagram bases catch keys that ignore how many times a letter appears.
    """
    letters = "abcdefghijklmnopqrstuvwxyz"
    bases = []
    for _ in range(max(1, n // 4)):
        if bases and rng.random() < 0.3:
            word = list(rng.choice(bases))
            i, j = rng.randrange(len(word)), rng.randrange(len(word))
            word[i] = word[j]
        else:
            word = [rng.choice(letters) for _ in range(rng.randint(3, 8))]
        bases.append("".join(word))
    strs = []
    for _ in range(n):
        word = list(rng.choice(bases))
        rng.shuffle(word)
        strs.append("".join(word))
    return {"strs": strs}


def is_valid(args):
    strs = args.get("strs")
    if not isinstance(strs, list) or not 1 <= len(strs) <= 10**5:
        return False
    for s in strs:
        if not isinstance(s, str) or len(s) > 10:
            return False
        if not all("a" <= c <= "z" for c in s):
            return False
    return True
