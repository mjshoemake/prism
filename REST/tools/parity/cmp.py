import sys
def parse(path):
    res, cur = {}, None
    for line in open(path):
        line = line.rstrip("\n")
        if line.startswith("##CASE "): cur = line[7:]; res[cur] = []
        elif line in ("PREPARE", "DEALLOCATE"): continue
        else: res[cur].append(line)
    return res
a, b = parse(sys.argv[1]), parse(sys.argv[2])
assert a.keys() == b.keys()
diff = [k for k in a if a[k] != b[k]]
print(f"{sys.argv[2]}: cases {len(a)}, rows {sum(map(len, a.values()))}, differing cases {len(diff)}")
by = {}
for k in diff: by[k.split(':')[2]] = by.get(k.split(':')[2], 0) + 1
if diff: print("  by metric:", by, "| e.g.", diff[0], a[diff[0]][:1], "->", b[diff[0]][:1])
