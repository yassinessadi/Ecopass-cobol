
# """
# Script: $.py

# Description:
#     Generates a synthetic Micro Focus-style binary test file
#     using the EcoPass 2-byte record header format.

# Author:
#     e7

# Maintained by:
#     e7
# """


import random
import string
from pathlib import Path

# Configuration
OUTPUT_FILE = Path("example/micro_20mb.dat")
TARGET_SIZE = 20 * 1024 * 1024  # 20 MiB

HEADER_SIZE = 2
ALIGNMENT = 2
MIN_PAYLOAD_SIZE = 512
MAX_PAYLOAD_SIZE = 2048

STATUSES = [4, 4, 4, 4, 3, 7]

random.seed(42)


def encode_header(status: int, length: int) -> bytes:
    """Encode a 2-byte EcoPass header."""

    if not 0 <= status <= 15:
        raise ValueError("Status must be between 0 and 15")

    if not 0 <= length <= 4095:
        raise ValueError("Payload length exceeds 2-byte header capacity")

    byte0 = ((status & 0x0F) << 4) | ((length >> 8) & 0x0F)
    byte1 = length & 0xFF

    return bytes([byte0, byte1])


def generate_payload(length: int, record_number: int) -> bytes:
    """Generate deterministic printable test data."""

    prefix = f"ECOPASS_RECORD_{record_number:010d}_"

    alphabet = string.ascii_uppercase + string.digits

    data = prefix + "".join(
        random.choices(alphabet, k=max(0, length - len(prefix)))
    )

    return data[:length].encode("ascii")


def calculate_padding(header_size: int, payload_length: int) -> int:
    total = header_size + payload_length
    return (ALIGNMENT - (total % ALIGNMENT)) % ALIGNMENT


def generate_file():
    OUTPUT_FILE.parent.mkdir(parents=True, exist_ok=True)

    total_bytes = 0
    record_count = 0
    status_counts = {}

    with open(OUTPUT_FILE, "wb") as output:

        while total_bytes < TARGET_SIZE:

            remaining = TARGET_SIZE - total_bytes

            # Each record has at least a 2-byte header.
            if remaining < HEADER_SIZE:
                raise RuntimeError("Remaining bytes cannot form a record")

            # Select a payload length that fits exactly in the remaining space
            # when approaching the end of the file.
            max_payload = min(
                MAX_PAYLOAD_SIZE,
                remaining - HEADER_SIZE
            )

            if max_payload < MIN_PAYLOAD_SIZE:
                payload_length = max_payload
            else:
                payload_length = random.randint(
                    MIN_PAYLOAD_SIZE,
                    max_payload
                )

            # Keep payload even-sized for 2-byte alignment.
            payload_length -= payload_length % ALIGNMENT

            if payload_length <= 0:
                raise RuntimeError("Invalid payload length")

            status = random.choice(STATUSES)

            header = encode_header(status, payload_length)

            payload = generate_payload(
                payload_length,
                record_count
            )

            padding_size = calculate_padding(
                HEADER_SIZE,
                payload_length
            )

            padding = b"\x00" * padding_size

            record = header + payload + padding

            if total_bytes + len(record) > TARGET_SIZE:
                raise RuntimeError("Record exceeds target file size")

            output.write(record)

            total_bytes += len(record)
            record_count += 1

            status_counts[status] = (
                status_counts.get(status, 0) + 1
            )

    print("=" * 50)
    print("EcoPass Micro File Generated")
    print("=" * 50)
    print(f"File:          {OUTPUT_FILE}")
    print(f"File size:     {total_bytes:,} bytes")
    print(f"File size:     {total_bytes / (1024 * 1024):.2f} MiB")
    print(f"Records:       {record_count:,}")
    print(f"Header size:   {HEADER_SIZE} bytes")
    print(f"Alignment:     {ALIGNMENT} bytes")
    print(f"Statuses:      {status_counts}")
    print("=" * 50)


if __name__ == "__main__":
    generate_file()
