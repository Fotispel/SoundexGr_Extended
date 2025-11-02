import pandas as pd
import matplotlib.pyplot as plt
import numpy as np

# Φόρτωση δεδομένων
df = pd.read_csv("results.csv")

methods = ["M1", "M2", "M3calc", "M3fix", "M4calc", "M4fix"]
colors = ["#1f77b4", "#ff7f0e", "#2ca02c", "#9467bd", "#8c564b", "#e377c2"]

x = np.arange(len(df["Dataset"]))
width = 0.13

plt.figure(figsize=(14, 6))
for i, method in enumerate(methods):
    f_col = f"{method}_F"
    len_col = f"{method}_len"
    plt.bar(x + i * width, df[f_col], width, label=method, color=colors[i])
    
    # Εμφάνιση μήκους πάνω από κάθε μπάρα
    for j, val in enumerate(df[f_col]):
        plt.text(x[j] + i * width, val + 0.005, str(df[len_col][j]), 
                 ha='center', va='bottom', fontsize=7, rotation=0)

plt.xticks(x + width * (len(methods) - 1) / 2, df["Dataset"], rotation=45, ha='right')
plt.ylabel("F-Score")
plt.ylim(0.6, 1.0)
plt.title("Comparative F-Score Results across Methods")
plt.legend(title="Method", ncol=3)
plt.tight_layout()
plt.show()
