import matplotlib.pyplot as plt

input_file = "erwtokritos_values.txt"

lengths = []
fscores = []

with open(input_file, "r") as file:
    for line in file:
        if line.strip():
            parts = line.strip().split()
            length = int(parts[1])
            fscore = float(parts[-1])
            lengths.append(length)
            fscores.append(fscore)

max_fscore = max(fscores)
max_index = fscores.index(max_fscore)
max_length = lengths[max_index]

plt.figure(figsize=(8, 5))
plt.plot(lengths, fscores, marker='o', linestyle='-', color='blue', label='Avg F-score')

# Highlight the max F-score with a red circle
plt.plot(max_length, max_fscore, 'ro', markersize=10, label=f'Max F-score = {max_fscore:.3f}')

plt.title("Progression of F-score over Code Length (Real-Time) for " + input_file)
plt.xlabel("Code Length")
plt.ylabel("Average F-score")
plt.grid(True)
plt.xticks(lengths)
plt.ylim(0, 1)
plt.legend()

plt.tight_layout()
plt.savefig("fscore_progress_real_time.png", dpi=300)
plt.show()