# SoundexGR Optimal Length Selection
On Finding the Optimal SoundexGR Code Length for Tackling the Out-of-Vocabulary Problem

This project builds upon the [SoundexGR algorithm](https://github.com/YannisTzitzikas/SoundexGR) proposed by Kavros and Tzitzikas (2023) for Greek phonetic matching. While SoundexGR achieves high accuracy, its performance depends on the chosen code length. Our work extends this by automatically selecting the optimal code length for a given dataset or task.

This repository contains the implementation and experimental evaluation of methods for automatically selecting the optimal SoundexGR code length for Greek phonetic matching.

## Methods Overview

### M1 – Exhaustive Search

* Tests a wide range of code lengths
* Selects the length with the highest average F-score
* Achieves highest accuracy but it is very expensive computationally

### M2 – Distinct-Words Based Heuristic

* Selects length based only on the number of distinct words
* Uses empirically derived intervals
* It is extremely fast (milliseconds) but may deviate from optimal length

### M3 – Range-Based Optimization

* Tests lengths within a range [3,12]
* Chooses the length whose **average collision list size** is closest to a target value `K`
* Supports:

  * dynamic `K`
  * fixed `K` (e.g. `K = 1.5`)
* It is fast and stable but achieves lower accuracy

### M4 – Hybrid (M2 and M3)

* Uses M2 to get an initial estimate
* Refines locally using M3-style optimization
* Best trade-off between accuracy and speed

## Evaluation

The methods were evaluated on 10 Greek datasets of varying size and genre:

* Technical texts
* Biographies
* Poems
* Theatrical plays
* Proses

Dataset sizes range from 80 to 9,000+ distinct words.

## Applicability

The proposed methods can be directly applied to:

* Document search (PDF/text viewers)
* Spell correction
* Autocompletion
* Entity Recognition
* Question Answering
* Structured data integration
* Phonetic user interfaces

## Author

**Yannis Tzitzikas**
Institute of Computer Science, FORTH, Heraklion, Crete, Greece

**Fotis Pelantakis**
Computer Science Department, University of Crete
