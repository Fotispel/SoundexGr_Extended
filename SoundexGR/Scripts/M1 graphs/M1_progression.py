import csv
import matplotlib.pyplot as plt

input_file = "java_m1_results.csv"

lengths = []
fscores = []

with open(input_file, newline="") as file:
    reader = csv.DictReader(file)
    for row in reader:
        lengths.append(int(row["Length"]))
        fscores.append(float(row["F-score"]))

max_fscore = max(fscores)
max_index = fscores.index(max_fscore)
max_length = lengths[max_index]

plt.figure(figsize=(8, 5))
plt.plot(lengths, fscores, marker='o', linestyle='-', label='Avg F-score')
plt.plot(max_length, max_fscore, 'o', markersize=10,
         label=f'Max F-score = {max_fscore:.3f}')

plt.title("Progression of F-score over Code Length (M1)")
plt.xlabel("Code Length")
plt.ylabel("Average F-score")
plt.grid(True)
plt.xticks(lengths)
plt.ylim(0, 1)
plt.legend()

plt.tight_layout()
plt.savefig("java_results.png", dpi=300)
plt.show()
