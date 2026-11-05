# Writes the command lists used by the scenarios: mobs spread over chunks round a spot, hoppers, farms.
import random

random.seed(7)
HOSTILE = ["zombie", "skeleton", "spider", "creeper", "husk", "stray", "drowned", "pillager", "vindicator", "witch"]
PASSIVE = ["cow", "sheep", "pig", "chicken", "villager", "rabbit", "wolf", "fox"]


def write(name, count, cx=0, cz=0, radius=90):
    with open(name, "w") as f:
        for i in range(count):
            kind = random.choice(HOSTILE if i % 3 else PASSIVE)
            x = cx + random.randint(-radius, radius)
            z = cz + random.randint(-radius, radius)
            f.write("summon minecraft:%s %d -60 %d {PersistenceRequired:1b}\n" % (kind, x, z))


write("mobs300.txt", 300)
write("mobs3000.txt", 3000)
