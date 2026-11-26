"""Start a tiny web server so a viewer can load out/transactions.json.

Browsers refuse to let a page opened straight from disk (file://) read other files,
so we serve the repo folder over http://localhost instead. Standard library only.

    python python-lane/tools/serve.py                      # the viewer in viewer/
    python python-lane/tools/serve.py --port 8001          # if 8000 is busy

Stop it with Ctrl+C.
"""

import argparse
import functools
import http.server
import webbrowser
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def main():
    p = argparse.ArgumentParser()
    p.add_argument("viewer", nargs="?", default="viewer", help="folder that holds index.html")
    p.add_argument("--port", type=int, default=8000)
    p.add_argument("--no-browser", action="store_true", help="do not open a browser tab")
    args = p.parse_args()

    folder = (ROOT / args.viewer).resolve()
    if not (folder / "index.html").exists():
        raise SystemExit(f"No index.html in {folder}. Is the path right?")

    handler = functools.partial(http.server.SimpleHTTPRequestHandler, directory=str(ROOT))
    server = http.server.ThreadingHTTPServer(("127.0.0.1", args.port), handler)
    url = f"http://localhost:{args.port}/{folder.relative_to(ROOT).as_posix()}/"
    print(f"Serving {ROOT}")
    print(f"Open {url}   (Ctrl+C to stop)")
    if not args.no_browser:
        webbrowser.open(url)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nStopped.")


if __name__ == "__main__":
    main()
