#!/usr/bin/env python3
"""
🦋 JDelta Generator: Enterprise Binary Differential Patch Generator.
Generates an optimized, cryptographically verified .delta patch file from two APK binaries.

Format:
- Magic: b'JDELTA01' (8 bytes)
- Source SHA-256: 32 bytes
- Target SHA-256: 32 bytes
- Target Length: 8 bytes (>Q)
- Compressed GZip Stream:
    - OP_COPY (0x01): src_offset (>Q), length (>I)
    - OP_INSERT (0x02): length (>I), raw_bytes
    - OP_END (0xFF)
"""

import sys
import os
import hashlib
import gzip
import struct
import io
import time

MAGIC = b'JDELTA01'
OP_COPY = 0x01
OP_INSERT = 0x02
OP_END = 0xFF

def sha256_file(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.digest()

def generate_patch(old_apk_path, new_apk_path, patch_path, block_size=4096):
    start_time = time.time()
    print(f"[JDelta] Generating binary patch...")
    print(f"  Old APK: {old_apk_path} ({os.path.getsize(old_apk_path):,} bytes)")
    print(f"  New APK: {new_apk_path} ({os.path.getsize(new_apk_path):,} bytes)")

    with open(old_apk_path, 'rb') as f:
        old_data = f.read()
    with open(new_apk_path, 'rb') as f:
        new_data = f.read()

    old_sha = hashlib.sha256(old_data).digest()
    new_sha = hashlib.sha256(new_data).digest()
    old_len = len(old_data)
    new_len = len(new_data)

    # 1. Index 4KB blocks in old_data
    print(f"[JDelta] Indexing source APK blocks (block_size={block_size})...")
    old_blocks = {}
    for i in range(0, old_len, block_size):
        chunk = old_data[i:i + block_size]
        h = hashlib.md5(chunk).digest()
        if h not in old_blocks:
            old_blocks[h] = i

    print(f"[JDelta] Scanned {len(old_blocks):,} unique blocks. Comparing deltas...")

    # 2. Generate operations
    raw_ops = bytearray()
    i = 0
    pending_insert = bytearray()
    matched_bytes = 0

    def flush_insert():
        nonlocal pending_insert
        if pending_insert:
            raw_ops.append(OP_INSERT)
            raw_ops.extend(struct.pack('>I', len(pending_insert)))
            raw_ops.extend(pending_insert)
            pending_insert = bytearray()

    while i < new_len:
        chunk = new_data[i:i + block_size]
        h = hashlib.md5(chunk).digest()
        if len(chunk) == block_size and h in old_blocks:
            flush_insert()
            src_offset = old_blocks[h]
            match_len = block_size

            # Greedy expansion for maximal contiguous chunk match
            while (i + match_len < new_len and 
                   src_offset + match_len < old_len and 
                   new_data[i + match_len] == old_data[src_offset + match_len]):
                match_len += 1

            raw_ops.append(OP_COPY)
            raw_ops.extend(struct.pack('>QI', src_offset, match_len))
            matched_bytes += match_len
            i += match_len
        else:
            pending_insert.append(new_data[i])
            i += 1

    flush_insert()
    raw_ops.append(OP_END)

    # 3. Compress operations stream with GZip level 9
    print(f"[JDelta] Matched {matched_bytes:,} / {new_len:,} bytes ({(matched_bytes/new_len)*100:.1f}%). Compressing...")
    compressed_ops = gzip.compress(bytes(raw_ops), compresslevel=9)

    # 4. Write formatted package
    os.makedirs(os.path.dirname(os.path.abspath(patch_path)), exist_ok=True)
    with open(patch_path, 'wb') as f:
        f.write(MAGIC)
        f.write(old_sha)
        f.write(new_sha)
        f.write(struct.pack('>Q', new_len))
        f.write(compressed_ops)

    patch_size = os.path.getsize(patch_path)
    elapsed = time.time() - start_time
    print(f"[JDelta] [SUCCESS] Patch generated in {elapsed:.2f}s:")
    print(f"  Patch file: {patch_path}")
    print(f"  Patch size: {patch_size:,} bytes ({patch_size / (1024 * 1024):.2f} MB)")
    print(f"  Bandwidth savings: {(1.0 - (patch_size / new_len)) * 100:.1f}%\n")
    return patch_size

if __name__ == '__main__':
    if len(sys.argv) < 4:
        print("Usage: python jdelta_generator.py <old_apk> <new_apk> <output_patch>")
        sys.exit(1)
    generate_patch(sys.argv[1], sys.argv[2], sys.argv[3])
