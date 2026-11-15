
# """
# Script: wtree.sh

# Description:
#     Generates a synthetic Micro Focus-style binary test file
#     using the EcoPass 2-byte record header format.

# Author:
#     Jane

# Maintained by:
#     Jane
# """

from pathlib import Path

data = Path("example/sample.dat").read_bytes()

print("File size:", len(data))
print("Hex:", data.hex(" "))

if len(data) >= 2:
    status = (data[0] >> 4) & 0x0F
    length_2 = ((data[0] & 0x0F) << 8) | data[1]

    print("\n2-byte header:")
    print("Status:", status)
    print("Payload length:", length_2)
    print("End without padding:", 2 + length_2)

if len(data) >= 4:
    length_4 = (
        ((data[0] & 0x0F) << 24)
        | (data[1] << 16)
        | (data[2] << 8)
        | data[3]
    )

    print("\n4-byte header:")
    print("Status:", status)
    print("Payload length:", length_4)
    print("End without padding:", 4 + length_4)