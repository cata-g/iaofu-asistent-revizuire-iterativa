def is_balanced(s: str) -> bool:
    count = 0
    for c in s:
        if c in "([{":
            count += 1
        else:
            count -= 1
            if count < 0:
                return False
    return count == 0
