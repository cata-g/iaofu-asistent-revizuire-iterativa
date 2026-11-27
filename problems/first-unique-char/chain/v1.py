def first_uniq_char(s: str) -> int:
    for i in range(len(s)):
        count = 0
        for c in s:
            if c == s[i]:
                count += 1
        if count == 1:
            return i
    return -1
