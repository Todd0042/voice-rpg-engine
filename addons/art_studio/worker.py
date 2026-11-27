"""Dual-machine resumable Art Queue worker (Laptop ComfyUI + Desktop Wan 2.2) with clean Pause/Resume."""

from __future__ import annotations

import json
from pathlib import Path
import threading
import time
from typing import Any

ACTIVE_ORDER = {"in_progress": 0, "pending": 1, "failed": 2, "completed": 3}


def sort_queue_items(items: list[dict[str, Any]]) -> list[dict[str, Any]]:
    """Order items: in_progress -> pending -> failed -> completed (always at bottom)."""
    return sorted(
        items,
        key=lambda x: (ACTIVE_ORDER.get(str(x.get("status") or "pending"), 2), str(x.get("wave") or "")),
    )


class ArtQueueManager:
    """Thread-safe queue state with graceful pause/resume across Laptop and Desktop workers."""

    def __init__(self, game_dir: Path) -> None:
        self.game_dir = game_dir
        self._lock = threading.RLock()
        self._stop_event = threading.Event()
        self._laptop_thread: threading.Thread | None = None
        self._desktop_thread: threading.Thread | None = None
        self.laptop_active_id: str = ""
        self.desktop_active_id: str = ""

    @property
    def queue_file(self) -> Path:
        return self.game_dir / "art/art_queue.json"

    def set_game_dir(self, game_dir: Path) -> None:
        with self._lock:
            self.pause()
            self.game_dir = game_dir

    def load(self) -> dict[str, Any]:
        with self._lock:
            if not self.queue_file.is_file():
                return {
                    "game": self.game_dir.name,
                    "gameDir": str(self.game_dir),
                    "paused": True,
                    "items": [],
                }
            data = json.loads(self.queue_file.read_text(encoding="utf-8"))
            # Reset any stale in_progress from an unclean crash back to pending
            if not self.is_running():
                for it in data.get("items") or []:
                    if it.get("status") == "in_progress":
                        it["status"] = "pending"
            return data

    def save(self, data: dict[str, Any]) -> None:
        with self._lock:
            self.queue_file.parent.mkdir(parents=True, exist_ok=True)
            data["updatedAt"] = time.time()
            tmp = self.queue_file.with_suffix(".json.tmp")
            tmp.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")
            tmp.replace(self.queue_file)

    def is_running(self) -> bool:
        return bool(
            (self._laptop_thread and self._laptop_thread.is_alive())
            or (self._desktop_thread and self._desktop_thread.is_alive())
        )

    def state_snapshot(self) -> dict[str, Any]:
        data = self.load()
        items = sort_queue_items(list(data.get("items") or []))
        counts = {
            "in_progress": sum(1 for x in items if x.get("status") == "in_progress"),
            "pending": sum(1 for x in items if x.get("status") == "pending"),
            "failed": sum(1 for x in items if x.get("status") == "failed"),
            "completed": sum(1 for x in items if x.get("status") == "completed"),
            "total": len(items),
        }
        l_paused = bool(data.get("laptopPaused", data.get("paused", True)))
        d_paused = bool(data.get("desktopPaused", data.get("paused", True)))
        return {
            "game": data.get("game") or self.game_dir.name,
            "gameDir": str(self.game_dir),
            "paletteColors": int(data.get("paletteColors") or 0),
            "paused": l_paused and d_paused,
            "laptopPaused": l_paused,
            "desktopPaused": d_paused,
            "running": self.is_running(),
            "laptopActive": self.laptop_active_id,
            "desktopActive": self.desktop_active_id,
            "counts": counts,
            "items": items,
        }

    def pause(self, machine: str = "all") -> dict[str, Any]:
        """Gracefully pause 'all', 'laptop', or 'desktop': finishes current in-flight item."""
        m = (machine or "all").strip().lower()
        with self._lock:
            data = self.load()
            if m in ("all", "laptop"):
                data["laptopPaused"] = True
            if m in ("all", "desktop"):
                data["desktopPaused"] = True
            data["paused"] = bool(data.get("laptopPaused", True) and data.get("desktopPaused", True))
            self.save(data)
            if data["paused"]:
                self._stop_event.set()
        return self.state_snapshot()

    def resume(self, runner_fn: Any = None, machine: str = "all") -> dict[str, Any]:
        """Resume 'all', 'laptop', or 'desktop' worker from next pending item."""
        m = (machine or "all").strip().lower()
        with self._lock:
            data = self.load()
            if m in ("all", "laptop"):
                data["laptopPaused"] = False
            if m in ("all", "desktop"):
                data["desktopPaused"] = False
            data["paused"] = bool(data.get("laptopPaused", True) and data.get("desktopPaused", True))
            self.save(data)
            self._stop_event.clear()
            if runner_fn is not None:
                if not data.get("laptopPaused", True) and not (self._laptop_thread and self._laptop_thread.is_alive()):
                    self._laptop_thread = threading.Thread(
                        target=self._worker_loop, args=("laptop", runner_fn), daemon=True
                    )
                    self._laptop_thread.start()
                if not data.get("desktopPaused", True) and not (self._desktop_thread and self._desktop_thread.is_alive()):
                    self._desktop_thread = threading.Thread(
                        target=self._worker_loop, args=("desktop", runner_fn), daemon=True
                    )
                    self._desktop_thread.start()
        return self.state_snapshot()

    def _next_ready_item(self, machine: str) -> dict[str, Any] | None:
        with self._lock:
            data = self.load()
            if data.get(f"{machine}Paused", data.get("paused", True)) or self._stop_event.is_set():
                return None
            items = data.get("items") or []
            done_ids = {it["id"] for it in items if it.get("status") == "completed"}
            for it in items:
                if it.get("machine") != machine or it.get("status") != "pending":
                    continue
                dep = it.get("dependsOn")
                if dep and dep not in done_ids:
                    continue
                it["status"] = "in_progress"
                self.save(data)
                return dict(it)
            return None

    def _finish_item(self, item_id: str, *, status: str, output_path: str = "", error: str = "", duration_sec: float = 0.0) -> None:
        with self._lock:
            data = self.load()
            for it in data.get("items") or []:
                if it.get("id") == item_id:
                    it["status"] = status
                    if output_path:
                        it["outputPath"] = output_path
                    it["error"] = error
                    it["durationSec"] = round(duration_sec, 1)
                    break
            self.save(data)

    def _worker_loop(self, machine: str, runner_fn: Any) -> None:
        while not self._stop_event.is_set():
            item = self._next_ready_item(machine)
            if not item:
                data = self.load()
                if data.get(f"{machine}Paused", data.get("paused", True)):
                    break
                still_active = any(
                    it.get("status") in ("pending", "in_progress") for it in (data.get("items") or [])
                )
                if not still_active:
                    break
                time.sleep(2.0)
                continue

            if machine == "laptop":
                self.laptop_active_id = item["id"]
            else:
                self.desktop_active_id = item["id"]

            t0 = time.time()
            try:
                out_path = runner_fn(self.game_dir, item)
                self._finish_item(
                    item["id"],
                    status="completed",
                    output_path=str(out_path or ""),
                    duration_sec=time.time() - t0,
                )
            except Exception as exc:
                self._finish_item(
                    item["id"],
                    status="failed",
                    error=str(exc)[:400],
                    duration_sec=time.time() - t0,
                )
            finally:
                if machine == "laptop":
                    self.laptop_active_id = ""
                else:
                    self.desktop_active_id = ""
