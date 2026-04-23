from __future__ import annotations

import argparse
import json
import math
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import List


SOCKET_MAX_FRAMES = 100


@dataclass(frozen=True)
class Pulse:
    frames: List[str]


@dataclass(frozen=True)
class SectionMeta:
    frequency_start: int
    frequency_end: int
    frame_count: int
    mode: int


def main() -> int:
    parser = argparse.ArgumentParser(
        description="Auto-convert all .pulse files in the script directory into .frame files."
    )
    parser.add_argument(
        "--dir",
        default=str(Path(__file__).resolve().parent),
        help="Directory to scan. Defaults to this script directory.",
    )
    args = parser.parse_args()
    scan_dir = Path(args.dir)
    if not scan_dir.is_dir():
        raise SystemExit(f"Directory not found: {scan_dir}")

    sources = sorted(scan_dir.glob("*.pulse"))
    if not sources:
        print(f"No .pulse files found in: {scan_dir}")
        return 0

    converted = 0
    for source in sources:
        pulse = parse_pulse_file(source)
        pulse = truncate_frames_if_needed(pulse, source)
        target = source.with_suffix(".frame")
        write_frame_file(target, pulse)
        print(f"Wrote {target}")
        converted += 1

    print(f"Converted {converted} file(s).")
    return 0


def parse_pulse_file(path: Path) -> Pulse:
    content = path.read_text(encoding="utf-8")
    return parse_pulse_content(content)


def parse_pulse_content(content: str) -> Pulse:
    text = content.strip()
    if not text:
        raise ValueError("Pulse file content is empty")

    if text.startswith("Dungeonlab+pulse:"):
        return parse_dungeonlab_pulse(text)
    if text.startswith("{"):
        return parse_json_pulse(text)
    return parse_key_value_pulse(text)


def parse_dungeonlab_pulse(text: str) -> Pulse:
    body = text[len("Dungeonlab+pulse:") :].strip()
    header_and_payload = body.split("=", 1)
    if len(header_and_payload) != 2:
        raise ValueError("Invalid Dungeonlab+pulse content")

    frames: List[str] = []
    for section in header_and_payload[1].split("+section+"):
        meta_text, points_text = split_section(section)
        meta = parse_section_meta(meta_text)
        levels = parse_levels(points_text)
        if not levels:
            continue

        target_frames = meta.frame_count if meta.frame_count > 0 else len(levels)
        frames.extend(resample_section(levels, target_frames, meta))

    if not frames:
        raise ValueError("No frames could be generated from the pulse content")

    pulse = Pulse(frames=frames)
    validate_pulse(pulse)
    return pulse


def split_section(section: str) -> tuple[str, str]:
    meta_and_points = section.split("/", 1)
    if len(meta_and_points) != 2:
        raise ValueError(f"Invalid section payload: {section}")
    return meta_and_points[0].strip(), meta_and_points[1].strip()


def parse_section_meta(meta_text: str) -> SectionMeta:
    parts = [part.strip() for part in meta_text.split(",")]
    frequency_start = parse_int(parts[0], 10) if len(parts) >= 1 else 10
    frequency_end = parse_int(parts[1], frequency_start) if len(parts) >= 2 else frequency_start
    frame_count = max(0, parse_int(parts[2], 0)) if len(parts) >= 3 else 0
    mode = max(1, parse_int(parts[3], 1)) if len(parts) >= 4 else 1
    return SectionMeta(frequency_start, frequency_end, frame_count, mode)


def parse_levels(points_text: str) -> List[int]:
    levels: List[int] = []
    for token in points_text.split(","):
        item = token.strip()
        if not item:
            continue
        percent_text = item.split("-", 1)[0].strip()
        try:
            percent = float(percent_text)
        except ValueError:
            continue
        levels.append(clamp_int(round(percent), 0, 100))
    return levels


def resample_section(levels: List[int], target_frames: int, meta: SectionMeta) -> List[str]:
    if target_frames <= 0:
        return []

    if len(levels) == 1:
        return [encode_frame(frequency_at(meta, i, target_frames), levels[0]) for i in range(target_frames)]

    out: List[str] = []
    for i in range(target_frames):
        source_index = math.floor(i * len(levels) / float(target_frames))
        source_index = clamp_int(source_index, 0, len(levels) - 1)
        level = levels[source_index]
        out.append(encode_frame(frequency_at(meta, i, target_frames), level))
    return out


def frequency_at(meta: SectionMeta, index: int, frame_count: int) -> int:
    start = clamp_frequency(meta.frequency_start)
    end = clamp_frequency(meta.frequency_end)
    if meta.mode <= 1 or frame_count <= 1:
        return start

    phase = (index / max(1, frame_count - 1)) * meta.mode
    local = phase - math.floor(phase)
    frequency = round(start * (1.0 - local) + end * local)
    return clamp_frequency(frequency)


def encode_frame(frequency: int, level: int) -> str:
    return f"{clamp_frequency(frequency):02X}" * 4 + f"{clamp_int(level, 0, 100):02X}" * 4


def clamp_frequency(value: int) -> int:
    return clamp_int(value, 10, 240)


def clamp_int(value: int, low: int, high: int) -> int:
    return max(low, min(high, value))


def parse_json_pulse(text: str) -> Pulse:
    obj = json.loads(text)
    if isinstance(obj, list):
        frames = [str(frame).strip().upper() for frame in obj]
    else:
        frames = [str(frame).strip().upper() for frame in obj.get("frames", [])]
    pulse = Pulse(frames=frames)
    validate_pulse(pulse)
    return pulse


def parse_key_value_pulse(text: str) -> Pulse:
    values = {"frames": []}
    for raw_line in text.splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            continue
        key, value = line.split("=", 1)
        key = key.strip().lower()
        value = value.strip()
        if key == "frames":
            values["frames"] = [item.strip().upper() for item in value.split(",") if item.strip()]

    pulse = Pulse(frames=list(values["frames"]))
    validate_pulse(pulse)
    return pulse


def validate_pulse(pulse: Pulse) -> None:
    if not pulse.frames:
        raise ValueError("Pulse frames cannot be empty")
    for frame in pulse.frames:
        if len(frame) != 16 or any(ch not in "0123456789ABCDEFabcdef" for ch in frame):
            raise ValueError(f"Invalid pulse frame: {frame}")


def truncate_frames_if_needed(pulse: Pulse, source: Path) -> Pulse:
    if len(pulse.frames) <= SOCKET_MAX_FRAMES:
        return pulse

    print(
        f"WARNING: {source.name} has {len(pulse.frames)} frames, truncated to {SOCKET_MAX_FRAMES}.",
        file=sys.stderr,
    )
    truncated_frames = pulse.frames[:SOCKET_MAX_FRAMES]
    return Pulse(frames=truncated_frames)


def write_frame_file(path: Path, pulse: Pulse) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    data = {
        "frames": pulse.frames,
    }
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def parse_int(value: str, fallback: int) -> int:
    try:
        return int(value)
    except ValueError:
        return fallback


if __name__ == "__main__":
    raise SystemExit(main())