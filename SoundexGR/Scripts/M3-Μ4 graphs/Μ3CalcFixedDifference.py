import pandas as pd
import matplotlib.pyplot as plt

df = pd.read_csv("M3_measurements.csv")

datasets_order = [
    "Java", "Turing", "Monogramma", "Youtube",
    "Seven Against Thebes", "Electra", "Oidypous Tyrannos",
    "Lysistrata", "Autumn", "Erwtokritos"
]

df["Dataset"] = df["Dataset"].str.strip()
df = df.set_index("Dataset").loc[datasets_order]

plt.figure(figsize=(10, 6))

x = range(len(df))

plt.scatter(
    x, df["M3fixed_F"],
    label="M3 (K = 1.5)",
    s=70
)

plt.scatter(
    x, df["M3calc_F"],
    label="M3 (K calculated)",
    s=70
)

# Vertical difference lines
for i, dataset in enumerate(df.index):
    y1 = df.loc[dataset, "M3fixed_F"]
    y2 = df.loc[dataset, "M3calc_F"]
    plt.plot([i, i], [y1, y2], linestyle="--", color="gray", alpha=0.7)

plt.title("F-Score Difference per Dataset (M3 Methods)", fontsize=13)
plt.xlabel("Dataset")
plt.ylabel("F-Score")
plt.xticks(x, df.index, rotation=25, ha="right")
plt.ylim(0.6, 0.95)
plt.grid(axis="y", linestyle="--", alpha=0.5)
plt.legend()

plt.tight_layout()
plt.show()
