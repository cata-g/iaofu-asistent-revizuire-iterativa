Problema principală este ordinea operațiilor din buclă: adaugi `nums[i]` în dicționar **înainte** să cauți `target - x`. Când `target - x == x`, găsești chiar elementul curent și returnezi `[i, i]`.

De exemplu, pentru `nums = [3, 2, 4]` și `target = 6`, funcția returnează `[0, 0]`.

Soluția corectă:

```python
def two_sum(nums, target):
    seen = {}
    for i, x in enumerate(nums):
        if target - x in seen:
            return [seen[target - x], i]
        seen[x] = i
```
