"""
Audio recording module using sounddevice.
Records from the default microphone and saves as WAV.
"""
from __future__ import annotations

import os
import signal
import sys
import threading
import time
from datetime import datetime
from pathlib import Path
from typing import Optional

import numpy as np
import sounddevice as sd
import soundfile as sf


def list_devices() -> None:
    """List all available audio input/output devices in a readable format."""
    devices = sd.query_devices()
    print(f"{'Index':<8} {'Name':<50} {'Channels':<10} {'Sample Rate':<15} {'Default'}")
    print("-" * 90)
    default_input = sd.default.device[0]
    default_output = sd.default.device[1]
    for i, dev in enumerate(devices):
        name = dev["name"]
        channels = f"{dev['max_input_channels']}in/{dev['max_output_channels']}out"
        samplerate = f"{dev['default_samplerate']:.0f} Hz"
        is_default = ""
        if i == default_input and i == default_output:
            is_default = "in/out"
        elif i == default_input:
            is_default = "input"
        elif i == default_output:
            is_default = "output"
        print(f"{i:<8} {name:<50} {channels:<10} {samplerate:<15} {is_default}")


def record(
    duration: Optional[float] = None,
    samplerate: int = 16000,
    device: Optional[int] = None,
) -> str:
    """
    Record audio from the default microphone.

    If duration is None, records until the user presses Enter.
    Otherwise, records for the specified number of seconds.

    Args:
        duration: Recording duration in seconds. None = record until Enter.
        samplerate: Sample rate in Hz (default: 16000 for Whisper).
        device: Input device index. None = default device.

    Returns:
        Path to the saved WAV file.
    """
    recordings_dir = Path.home() / "sonar-recordings"
    recordings_dir.mkdir(parents=True, exist_ok=True)

    # Generate a timestamp-based filename
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    output_path = recordings_dir / f"recording_{timestamp}.wav"

    print(f"🎤 录音中... (采样率: {samplerate} Hz)")
    if duration:
        print(f"   时长: {duration} 秒")
    else:
        print("   按 Enter 停止录音")

    recorded_data = []
    stop_recording = threading.Event()

    def callback(indata, frames, time_info, status):
        """Stream callback -- called for each audio block."""
        if status:
            print(f"   状态: {status}", file=sys.stderr)
        recorded_data.append(indata.copy())

    def wait_for_enter():
        """Wait for the user to press Enter."""
        input()
        stop_recording.set()

    # If no duration, start a thread to wait for Enter
    enter_thread = None
    if duration is None:
        enter_thread = threading.Thread(target=wait_for_enter, daemon=True)
        enter_thread.start()

    # Start the stream
    stream = sd.InputStream(
        samplerate=samplerate,
        device=device,
        channels=1,
        callback=callback,
    )

    stream.start()

    try:
        if duration is not None:
            time.sleep(duration)
        else:
            stop_recording.wait()
    except KeyboardInterrupt:
        print("\n⏹️  录音被中断")
    finally:
        stream.stop()
        stream.close()

    if not recorded_data:
        raise RuntimeError("未捕获到音频数据，请检查麦克风连接")

    # Concatenate all audio chunks
    audio = np.concatenate(recorded_data, axis=0)

    # Save as WAV
    sf.write(str(output_path), audio, samplerate)
    file_size = os.path.getsize(output_path)
    duration_secs = len(audio) / samplerate
    print(f"✅ 录音已保存: {output_path}")
    print(f"   时长: {duration_secs:.1f} 秒, 大小: {file_size / 1024:.1f} KB")

    return str(output_path)
