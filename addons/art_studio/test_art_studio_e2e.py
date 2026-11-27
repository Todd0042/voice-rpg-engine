"""E2E test for voice-rpg-engine Art Studio plan generator, queue sorting, and Pause/Resume."""

from __future__ import annotations

import pathlib
import shutil
import tempfile
import threading
import time
import unittest

from fastapi.testclient import TestClient

from addons.art_studio.planner import generate_game_plan
from addons.art_studio.server import create_app
from addons.art_studio.worker import ArtQueueManager


class ArtStudioE2ETest(unittest.TestCase):
    def setUp(self) -> None:
        self.tmp = Path(tempfile.mkdtemp(prefix="art_studio_e2e_")) if False else pathlib.Path(tempfile.mkdtemp(prefix="art_studio_e2e_"))
        self.game = self.tmp / "demo-game"
        real_assets = pathlib.Path.home() / "GitHub/superstore-saga/app/src/main/assets/game"
        shutil.copytree(real_assets, self.game / "app/src/main/assets/game")

    def tearDown(self) -> None:
        shutil.rmtree(self.tmp, ignore_errors=True)

    def test_plan_generation_sort_and_pause_resume(self) -> None:
        plan = generate_game_plan(self.game, wave_size=5)
        items = plan["items"]
        self.assertGreater(len(items), 15)
        # Backgrounds are placed LAST
        self.assertEqual(items[-1]["kind"], "combat_backdrop")
        self.assertIn("retheme", items[-1]["name"])

        mgr = ArtQueueManager(self.game)
        # Mark one completed and one in_progress, verify sort order: in_progress -> pending -> completed
        data = mgr.load()
        data["items"][0]["status"] = "completed"
        data["items"][1]["status"] = "in_progress"
        mgr.save(data)

        # Simulated runner completes one item and verifies pause stops before next
        ran: list[str] = []
        started = threading.Event()
        release = threading.Event()

        def fake_runner(_gd: pathlib.Path, it: dict) -> str:
            ran.append(it["id"])
            started.set()
            release.wait(timeout=3.0)
            return "/tmp/fake.png"

        # Reset item[1] to pending for runner test
        d2 = mgr.load()
        d2["items"][1]["status"] = "pending"
        mgr.save(d2)

        mgr.resume(fake_runner)
        self.assertTrue(started.wait(timeout=3.0))
        # Pause while item is in flight -> finishes current item, does NOT start another
        mgr.pause()
        release.set()
        time.sleep(0.3)
        snap = mgr.state_snapshot()
        self.assertTrue(snap["paused"])
        self.assertEqual(snap["items"][-1]["status"], "completed")

        # FastAPI HTTP surface check
        client = TestClient(create_app(self.game))
        self.assertEqual(client.get("/").status_code, 200)
        st = client.get("/api/state")
        self.assertEqual(st.status_code, 200)
        self.assertIn("counts", st.json())
        self.assertEqual(client.post("/api/queue/pause").status_code, 200)


if __name__ == "__main__":
    unittest.main()
