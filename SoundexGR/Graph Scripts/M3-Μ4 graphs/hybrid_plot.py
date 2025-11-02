import pandas as pd
import matplotlib.pyplot as plt

# Φόρτωση δεδομένων
df = pd.read_csv('m4_results.txt')

# Pivot για να έχουμε κάθε μέθοδο ως στήλη
pivot_df = df.pivot(index='Dataset', columns='Method', values='F-Score')

# Διατήρηση σταθερής σειράς datasets
datasets_order = [
    "Java", "Turing", "Monogramma", "Youtube",
    "Seven against thebes", "Electra", "Oidypous tyrannos",
    "Lysistrata", "Autumn", "Erwtokritos"
]
pivot_df = pivot_df.loc[datasets_order]

# Δημιουργία διαγράμματος
plt.figure(figsize=(10, 6))

# Θέσεις στον οριζόντιο άξονα
x = range(len(pivot_df))

# Σημεία για κάθε μέθοδο
plt.scatter(x, pivot_df["Hybrid_ii-iii_fixedK"], color='orange', label="Hybrid ii-iii (K = 1.5)", s=70)
plt.scatter(x, pivot_df["Hybrid_ii-iii_K"], color='blue', label="Hybrid ii-iii (K calculated)", s=70)

# Κάθετες γραμμές για να δείχνουν τη διαφορά
for i, dataset in enumerate(pivot_df.index):
    y1 = pivot_df.loc[dataset, "Hybrid_ii-iii_fixedK"]
    y2 = pivot_df.loc[dataset, "Hybrid_ii-iii_K"]
    plt.plot([i, i], [y1, y2], color='gray', linestyle='--', alpha=0.7)

# Ετικέτες και ρυθμίσεις
plt.title("F-Score Difference per Dataset (Hybrid ii-iii Methods)", fontsize=13)
plt.xlabel("Dataset")
plt.ylabel("F-Score")
plt.xticks(x, pivot_df.index, rotation=25, ha='right')
plt.ylim(0.6, 0.95)  # λίγο πιο κάτω γιατί το Erwtokritos έχει 0.65
plt.grid(axis='y', linestyle='--', alpha=0.5)
plt.legend()

plt.tight_layout()
plt.show()
