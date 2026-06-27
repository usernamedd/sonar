"""
Speech-to-text module using faster-whisper.
Transcribes audio files with support for Chinese and English.
"""
from __future__ import annotations

import sys
from pathlib import Path
from typing import Dict, List, Optional, Tuple

import numpy as np


def transcribe(
    audio_path: str,
    model_size: str = "base",
    device: str = "cpu",
    language: Optional[str] = None,
) -> Dict:
    """
    Transcribe an audio file using faster-whisper.

    Args:
        audio_path: Path to the audio file (WAV, MP3, etc.).
        model_size: Whisper model size ("tiny", "base", "small", "medium", "large").
        device: Device to run inference on ("cpu" or "cuda").
        language: Language code ("zh" for Chinese, "en" for English, None for auto-detect).

    Returns:
        Dict with keys:
            - "text": Full transcribed text.
            - "segments": List of segment dicts with "start", "end", "text".
            - "language": Detected or specified language.
    """
    audio_path = str(Path(audio_path).expanduser().resolve())
    if not Path(audio_path).exists():
        raise FileNotFoundError(f"音频文件不存在: {audio_path}")

    print(f"📝 正在转录音频: {Path(audio_path).name}")
    print(f"   模型: {model_size}, 设备: {device}")

    try:
        from faster_whisper import WhisperModel
    except ImportError:
        print("错误: 请安装 faster-whisper: pip install faster-whisper", file=sys.stderr)
        raise

    # Load model (auto-downloads on first use)
    print(f"   加载模型 '{model_size}'...")
    model = WhisperModel(model_size, device=device, compute_type="float32")

    # Run transcription
    print(f"   正在识别...")
    segments_info, info = model.transcribe(audio_path, language=language, beam_size=5)

    detected_language = info.language if info else "unknown"
    print(f"   检测到的语言: {detected_language} (概率: {info.language_probability:.2%})")

    # Collect results
    full_text_parts = []
    segments_list = []

    for seg in segments_info:
        segment_data = {
            "start": seg.start,
            "end": seg.end,
            "text": seg.text.strip(),
        }
        segments_list.append(segment_data)
        full_text_parts.append(seg.text.strip())

    full_text = " ".join(full_text_parts)

    print(f"✅ 转录完成! 文本长度: {len(full_text)} 字符")

    return {
        "text": full_text,
        "segments": segments_list,
        "language": detected_language,
    }
