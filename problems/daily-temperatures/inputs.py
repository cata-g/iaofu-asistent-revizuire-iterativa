def generate(n, rng):
    """A non-increasing prefix (80%) of warm days, then random colder days.

    No day of the prefix is followed by a warmer one, so a nested scan is quadratic.
    The prefix has long runs of equal temperatures, so treating equal as warmer is caught.
    """
    prefix = n * 4 // 5
    warm = sorted((rng.randint(21, 50) for _ in range(prefix)), reverse=True)
    cold = [rng.randint(-50, 20) for _ in range(n - prefix)]
    return {"temperatures": warm + cold}


def is_valid(args):
    if not isinstance(args, dict):
        return False
    temperatures = args.get("temperatures")
    if not isinstance(temperatures, list) or not 1 <= len(temperatures) <= 10**5:
        return False
    return all(isinstance(t, int) and not isinstance(t, bool) and -50 <= t <= 50 for t in temperatures)
