## Step 2 (REVISED) — Get the Dataset: Teklia/IAM-line via Hugging Face

> **This replaces the original Step 2**, which assumed access to the official IAM registration site. If that's blocked or slow for you, use this instead — it's the same underlying IAM data, pre-split and line-level, with no registration wall.

### Purpose
Obtain the same line-level handwritten text data the original guide needed, through a source that doesn't require an approval-gated account.

### Why Note2Snap needs this
Functionally identical goal to the original Step 2: a large set of `(image, transcription)` pairs to pretrain the CRNN before whiteboard-specific fine-tuning. `Teklia/IAM-line` is a direct Hugging Face mirror of IAM already split into line-level train/validation/test sets — no parsing of `lines.txt`/folder structures needed at all.

### What to run (new Colab cell)
```python
!pip install datasets -q
```

```python
from datasets import load_dataset

iam_dataset = load_dataset("Teklia/IAM-line")
print(iam_dataset)
print(iam_dataset["train"][0])
```

### Code walkthrough
- **`datasets` library** is Hugging Face's standard dataset-loading tool — one `load_dataset(...)` call handles downloading, caching, and giving you ready-to-use train/validation/test splits, replacing the manual `.tgz` extraction and `lines.txt` parsing the original Step 2/3 needed.
- **No account, login, or approval wait**: this dataset is published openly on Hugging Face's hub.
- **Images arrive as PIL Images already decoded**, and each sample is a dict with `'image'` and `'text'` keys — this is a cleaner starting shape than IAM's raw folder structure, so Step 3's parsing work below is much shorter than the original.
- Note: IAM's underlying license (non-commercial/research use) still applies regardless of which mirror you pull it from — same licensing note as before applies for your capstone documentation.

### How to test this step
Run the two cells above.

### Expected output
```
DatasetDict({
    train: Dataset({features: ['image', 'text'], num_rows: 6482})
    validation: Dataset({features: ['image', 'text'], num_rows: 976})
    test: Dataset({features: ['image', 'text'], num_rows: 2915})
})
{'image': <PIL.Image.Image ...>, 'text': 'put down a resolution on the subject'}
```

---

## Step 3 (REVISED) — Build the Character Set from the Hugging Face Dataset

### Purpose
Same purpose as the original Step 3 — determine the exact character set your model needs — just adapted to this dataset's already-clean format.

### Complete code (new Colab cell)
```python
import os

# Convert to a flat list of (PIL Image, transcription) pairs, matching the
# `samples` shape the rest of the guide (Steps 4 onward) expects.
train_samples_raw = [(item["image"], item["text"]) for item in iam_dataset["train"]]
val_samples_raw = [(item["image"], item["text"]) for item in iam_dataset["validation"]]

all_characters = set()
for _, transcription in train_samples_raw + val_samples_raw:
    all_characters.update(transcription)

charset = sorted(all_characters)
print(f"Charset size: {len(charset)}")
print("Charset:", "".join(charset))
```

Save it the same way as before:
```python
import json

os.makedirs("/content/drive/MyDrive/note2snap_crnn", exist_ok=True)
charset_path = "/content/drive/MyDrive/note2snap_crnn/charset.json"
with open(charset_path, "w") as f:
    json.dump(charset, f)

print("Saved charset to", charset_path)
```

### One adjustment needed for Step 4
The original Step 4's `load_and_preprocess_image` function reads images from a file path (`tf.io.read_file(image_path)`), but this dataset gives you **PIL Image objects already in memory**, not file paths. Replace Step 4's image-loading approach with this version:

```python
import numpy as np
import tensorflow as tf

IMG_HEIGHT = 32
IMG_WIDTH = 256

def preprocess_pil_image(pil_image):
    grayscale = pil_image.convert("L")  # matches Android's grayscale preprocessing
    resized = grayscale.resize((IMG_WIDTH, IMG_HEIGHT))
    array = np.array(resized, dtype=np.float32) / 255.0
    return array[..., np.newaxis]  # add channel dimension -> (H, W, 1)

# Pre-convert everything once (fine for IAM-line's size; keeps the rest of
# Step 4's build_dataset function structurally the same, just fed arrays
# instead of file paths).
train_images_np = np.stack([preprocess_pil_image(img) for img, _ in train_samples_raw])
val_images_np = np.stack([preprocess_pil_image(img) for img, _ in val_samples_raw])

train_samples = list(zip(train_images_np, [t for _, t in train_samples_raw]))
val_samples = list(zip(val_images_np, [t for _, t in val_samples_raw]))
```

> With this change, when you reach Step 4's `build_dataset` function, replace the `image_ds = tf.data.Dataset.from_tensor_slices(image_paths).map(load_and_preprocess_image, ...)` line with:
> ```python
> image_ds = tf.data.Dataset.from_tensor_slices(np.stack([s[0] for s in samples]))
> ```
> since your images are already preprocessed NumPy arrays at this point, not paths needing decoding.

### Code walkthrough
- **`grayscale.resize((IMG_WIDTH, IMG_HEIGHT))`**: PIL's `resize` takes `(width, height)` order — easy to get backwards, worth double-checking since a swapped width/height here silently distorts every training image.
- **Pre-converting all images to NumPy arrays upfront** (rather than lazily inside the `tf.data` pipeline like the original file-path version did) is reasonable at IAM-line's size (~7,400 train+val images) and keeps the rest of the guide's `tf.data` structure intact with minimal changes — for a much larger dataset you'd want to keep this lazy instead, but it's not necessary here.
- Everything from **Step 5 onward (model architecture, CTC loss, training, evaluation, fine-tuning, export) is unchanged** — this substitution only affects how images get loaded, not anything about the model or training process.

### How to test this step
Run the cells, then verify shapes:
```python
print(train_images_np.shape)  # expect (6482, 32, 256, 1)
print(train_samples[0][1])    # a transcription string
```

### Expected output
```
(6482, 32, 256, 1)
'put down a resolution on the subject'
```

---

## Step 4 (REVISED) — build_dataset, adapted for in-memory images

> **This replaces the `build_dataset` function from the original guide's Step 4.** The original version assumed `samples` was a list of `(file_path_string, transcription)` pairs, and loaded each image from disk. Since Step 3 (revised) already converted every image into an in-memory pixel array, there's no file left to open — this version skips that step and feeds the arrays straight into TensorFlow. Everything else about the data pipeline (label encoding, batching, shuffling, prefetching) is unchanged from the original.

### Complete code (new Colab cell — use this instead of the original Step 4's `build_dataset`)
```python
import numpy as np
import tensorflow as tf

def encode_label(transcription, max_label_length=64):
    indices = [char_to_index[char] for char in transcription if char in char_to_index]
    length = len(indices)
    padded = indices + [0] * (max_label_length - length)
    return np.array(padded[:max_label_length], dtype=np.int32), min(length, max_label_length)


def build_dataset(samples, batch_size=32, shuffle=True):
    images = np.stack([s[0] for s in samples])       # s[0] = pixel array (already preprocessed)
    transcriptions = [s[1] for s in samples]          # s[1] = the text label

    encoded_labels = []
    label_lengths = []
    for t in transcriptions:
        encoded, length = encode_label(t)
        encoded_labels.append(encoded)
        label_lengths.append(length)

    # Images are already numpy arrays at this point (from Step 3 revised),
    # so we load them straight in — no file-reading/decoding step needed here.
    image_ds = tf.data.Dataset.from_tensor_slices(images)

    label_ds = tf.data.Dataset.from_tensor_slices(np.array(encoded_labels))
    label_length_ds = tf.data.Dataset.from_tensor_slices(np.array(label_lengths, dtype=np.int32))

    dataset = tf.data.Dataset.zip((image_ds, label_ds, label_length_ds))
    if shuffle:
        dataset = dataset.shuffle(buffer_size=1000, seed=42)
    dataset = dataset.batch(batch_size).prefetch(tf.data.AUTOTUNE)
    return dataset


char_to_index = {char: index + 1 for index, char in enumerate(charset)}  # 0 reserved for CTC blank
index_to_char = {index: char for char, index in char_to_index.items()}

train_dataset = build_dataset(train_samples, batch_size=32, shuffle=True)
val_dataset = build_dataset(val_samples, batch_size=32, shuffle=False)

print("Train batches:", tf.data.experimental.cardinality(train_dataset).numpy())
print("Val batches:", tf.data.experimental.cardinality(val_dataset).numpy())
```

### What changed vs. the original Step 4, side by side

| | Original (file-based) | This version (in-memory) |
|---|---|---|
| What `samples` contains | `(file_path_string, text)` | `(pixel_array, text)` |
| How images get loaded | `tf.io.read_file(path)` then decode | Already loaded — wrap directly in a `Dataset` |
| `load_and_preprocess_image` function | Used | Not needed — skip it entirely |
| `IMG_HEIGHT`, `IMG_WIDTH` constants | Defined in Step 4 | Already defined earlier, in Step 3 (revised) — don't redefine |
| `char_to_index` / `index_to_char` | Defined in Step 4 | Same — kept here too, so this cell is self-contained |

### Code walkthrough
- **`np.stack([s[0] for s in samples])`**: `samples` is a list of `(pixel_array, text)` tuples; this pulls out just the pixel arrays and stacks them into one big NumPy array of shape `(num_samples, 32, 256, 1)` — the format `tf.data.Dataset.from_tensor_slices` expects.
- **No `.map(load_and_preprocess_image, ...)` call anymore**: that `.map()` step existed specifically to turn file paths into pixels. Since Step 3 (revised) already did that conversion once, up front, applying it again here would either crash (wrong input type) or redundantly reprocess the same data.
- **`encode_label` and the `char_to_index`/`index_to_char` dictionaries are unchanged** from the original guide — label encoding doesn't care where the image came from, so nothing about the CTC label logic needed to change.

### How to test this step
```python
for images, labels, label_lengths in train_dataset.take(1):
    print("Image batch shape:", images.shape)
    print("Label batch shape:", labels.shape)
    print("First label indices:", labels[0].numpy())
    print("First label length:", label_lengths[0].numpy())
    decoded = "".join(index_to_char[i] for i in labels[0].numpy() if i != 0)
    print("Decoded back to text:", decoded)
```

### Expected output
```
Image batch shape: (32, 32, 256, 1)
Label batch shape: (32, 64)
First label indices: [23 15 22 5 ...  0  0  0]
First label length: 18
Decoded back to text: (matches one of the original transcriptions from that batch)
```

From here, continue with the original guide's **Step 5 (model architecture) onward exactly as written** — nothing past this point needs to change.
