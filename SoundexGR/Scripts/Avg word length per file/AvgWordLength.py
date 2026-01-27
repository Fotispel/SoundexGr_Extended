from pathlib import Path


def avg_word_length_per_file():
    files = [
        "Autumn_words.txt", "Electra_words.txt", "Erwtokritos_words.txt",
        "java_words.txt", "Lysistrata_words.txt", "Monogramma_words.txt",
        "Oidypous%20Tyrannos_words.txt", "Seven%20Against%20Thebes_words.txt",
        "turing_words.txt", "Youtube_words.txt"
    ]

    base_dir = Path(__file__).resolve().parents[2]  # SoundexGR/
    data_dir = base_dir / "Resources" / "collection_words"

    for filename in files:
        path = data_dir / filename
        with open(path, "r", encoding="utf-8") as f:
            words = [line.strip() for line in f if line.strip()]

        avg = sum(len(w) for w in words) / len(words) if words else 0
        print(f"{filename}: {avg:.2f}")


if __name__ == "__main__":
    avg_word_length_per_file()