#!/usr/bin/env python3
"""Polite scraper for recipes on angingon.com (used with the site's permission).

Collects recipe URLs from the category listing pages (/danh-muc/<slug>/<page>),
fetches recipe pages one at a time with a delay, keeps only real single-dish
recipes (ingredient table + numbered steps) and writes one JSON object per line.
Re-running resumes where the last run stopped.

Usage:
    python3 scraper.py --list-categories
    python3 scraper.py --categories mon-chao,mon-nuoc,mon-chay --per-category 100
    python3 scraper.py --categories all --per-category 60
    python3 scraper.py --validate                # check every line of the JSONL file
    python3 scraper.py --repair                  # re-apply cleaning rules, no fetching
    python3 scraper.py --export-csv              # JSONL -> data/*.csv
    python3 scraper.py --test-url <URL>          # parse one page, no file written
"""

import argparse
import csv
import json
import random
import re
import sys
import time
import urllib.robotparser
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

import requests
from bs4 import BeautifulSoup

BASE = "https://www.angingon.com"
SITEMAP_STATIC = f"{BASE}/sitemap-static.xml"
USER_AGENT = "WikiCook-student-project/1.0 (+course project, scraping with permission)"
TIMEOUT = 20

# Categories that are not dishes; never scraped even with --categories all.
EXCLUDED_CATEGORIES = {"meo-vao-bep"}

# Longest first so "muỗng canh" wins over "muỗng". "ít"/"nhúm" mean "a little", not a measured amount.
UNITS = [
    "muỗng canh", "muỗng cà phê", "muỗng cafe", "muỗng café", "muỗng", "thìa canh", "thìa cà phê",
    "thìa cafe", "thìa", "chén", "bát", "tô", "ly", "cốc", "tách", "chai", "ống",
    "kg", "gram", "gr", "g", "ml", "lít", "l", "quả", "trái", "củ", "cây", "nhánh", "tép",
    "lá", "bó", "gói", "hộp", "lon", "miếng", "lát", "con", "cái", "tai", "khúc", "hạt", "viên",
    "ít", "nhúm",
]
UNIT_RE = re.compile(
    r"^\s*(?:khoảng\s+)?(?P<qty>\d+(?:[.,]\d+)?(?:\s*/\s*\d+)?|\d+\s*[-–]\s*\d+)?\s*(?P<unit>"
    + "|".join(sorted((re.escape(u) for u in UNITS), key=len, reverse=True))
    + r")?(?=\s|$|[.,;)])\s*(?P<rest>.*)$",
    re.IGNORECASE,
)

REQUIRED_FIELDS = {
    "source": str, "source_url": str, "slug": str, "title": str, "category": str,
    "category_slug": str, "ingredients": list, "steps": list, "scraped_at": str,
}
INGREDIENT_FIELDS = {"name", "note", "amount_text", "quantity", "unit", "group"}


# ---------------------------------------------------------------- fetching

class Fetcher:
    def __init__(self, delay, jitter=0.5, max_retries=4):
        self.delay = delay
        self.jitter = jitter
        self.max_retries = max_retries
        self.session = requests.Session()
        self.session.headers.update({"User-Agent": USER_AGENT, "Accept-Language": "vi,en;q=0.8"})
        self.last = 0.0
        self.robots = urllib.robotparser.RobotFileParser(f"{BASE}/robots.txt")
        self.robots.read()

    def get(self, url):
        if not self.robots.can_fetch(USER_AGENT, url):
            raise PermissionError(f"robots.txt disallows {url}")
        for attempt in range(1, self.max_retries + 1):
            wait = self.delay + random.uniform(0, self.jitter) - (time.monotonic() - self.last)
            if wait > 0:
                time.sleep(wait)
            self.last = time.monotonic()
            try:
                resp = self.session.get(url, timeout=TIMEOUT)
            except requests.RequestException as exc:
                print(f"  ! network error ({exc}); retry {attempt}", file=sys.stderr)
                time.sleep(self.delay * 2 ** attempt)
                continue
            if resp.status_code == 200:
                resp.encoding = "utf-8"
                return resp.text
            if resp.status_code in (403, 401):
                raise PermissionError(f"HTTP {resp.status_code} for {url}: stopping to respect the site")
            if resp.status_code == 404:
                return None
            if resp.status_code in (429, 500, 502, 503, 504):
                retry_after = resp.headers.get("Retry-After")
                pause = int(retry_after) if retry_after and retry_after.isdigit() else self.delay * 2 ** attempt
                print(f"  ! HTTP {resp.status_code}; waiting {pause:.0f}s", file=sys.stderr)
                time.sleep(pause)
                continue
            print(f"  ! HTTP {resp.status_code} for {url}; skipped", file=sys.stderr)
            return None
        return None


# ---------------------------------------------------------------- URL discovery

# Round-ups ("7-mon-chao-...") and multi-method pages ("2-cach-nau-...") mix several dishes
# into one ingredient table and one step list, so they are not usable as a single recipe.
MULTI_DISH = re.compile(r"^\d+-(mon|cach|kieu|loai)-")
NOT_A_DISH = re.compile(r"bao-nhieu-calo|tac-dung|nen-sam|la-gi|o-dau|review|dia-chi|meo-|bi-quyet")


def is_single_dish(url):
    slug = url.rstrip("/").rsplit("/", 1)[-1]
    return not MULTI_DISH.match(slug) and not NOT_A_DISH.search(slug)


def all_categories(fetcher):
    """Category slugs listed in the static sitemap, minus non-dish categories."""
    sitemap = BeautifulSoup(fetcher.get(SITEMAP_STATIC), "xml")
    slugs = []
    for loc in sitemap.find_all("loc"):
        m = re.fullmatch(rf"{re.escape(BASE)}/danh-muc/([a-z0-9-]+)", loc.text.strip())
        if m and m.group(1) not in EXCLUDED_CATEGORIES:
            slugs.append(m.group(1))
    return slugs


def category_page(fetcher, slug, page):
    """Recipe URLs on one listing page, the category's display name, total count and last page."""
    url = f"{BASE}/danh-muc/{slug}" + (f"/{page}" if page > 1 else "")
    html = fetcher.get(url)
    if not html:
        return [], None, None, 0
    soup = BeautifulSoup(html, "html.parser")
    urls = []
    for a in soup.select("article h2 a[href^='/cong-thuc/']"):
        full = BASE + a["href"].split("?")[0].split("#")[0]
        if full not in urls:
            urls.append(full)
    pages = [int(n) for n in re.findall(rf'href="/danh-muc/{re.escape(slug)}/(\d+)"', html)]
    title = soup.title.get_text(strip=True) if soup.title else ""
    total = re.match(r"\s*(\d+)\s", title)
    h1 = soup.find("h1")
    return urls, h1.get_text(" ", strip=True) if h1 else slug, int(total.group(1)) if total else None, max(pages + [page])


def category_urls(fetcher, slug):
    """Yield every recipe URL of a category, page by page, in the site's order."""
    page, last = 1, 1
    while page <= last:
        urls, _, _, last = category_page(fetcher, slug, page)
        if not urls:
            return
        yield from urls
        page += 1


# ---------------------------------------------------------------- parsing

def to_minutes(text):
    if not text:
        return None
    hours = re.search(r"(\d+)\s*giờ", text)
    mins = re.search(r"(\d+)\s*phút", text)
    if not hours and not mins:
        return None
    return (int(hours.group(1)) * 60 if hours else 0) + (int(mins.group(1)) if mins else 0)


def parse_amount(text):
    text = " ".join(text.split())
    m = UNIT_RE.match(text)
    qty = unit = None
    if m:
        raw = m.group("qty")
        if raw:
            raw = raw.replace(",", ".").replace(" ", "")
            if "/" in raw:
                num, den = raw.split("/", 1)
                qty = round(float(num) / float(den), 3) if float(den) else None
            elif "-" in raw or "–" in raw:
                lo, hi = re.split("[-–]", raw, maxsplit=1)
                qty = round((float(lo) + float(hi)) / 2, 3)
            else:
                qty = float(raw)
        unit = m.group("unit").lower() if m.group("unit") else None
        if unit in ("ít", "nhúm"):
            qty = None
    return {"amount_text": text, "quantity": qty, "unit": unit}


def split_name(raw):
    """'Đậu hũ (loại vàng hoặc trắng)' -> ('Đậu hũ', 'loại vàng hoặc trắng')."""
    raw = " ".join(raw.split())
    m = re.fullmatch(r"(?P<name>[^()]+?)\s*\((?P<note>[^()]*)\)\s*", raw)
    if m and m.group("name"):
        return m.group("name"), m.group("note").strip() or None
    return raw, None


def is_link_list(tag):
    """True for the 'related recipes' blocks: almost all of the text sits inside links."""
    text = tag.get_text(" ", strip=True)
    links = " ".join(a.get_text(" ", strip=True) for a in tag.find_all("a"))
    return bool(text) and len(links) >= 0.8 * len(text)


VIDEO_RE = re.compile(r"^https?://(www\.)?(youtube\.com|youtu\.be|tiktok\.com)/\S+$")


def step_body(h3):
    """Text, images and video links of one step, without the 'Xem thêm' blocks and the 'Món liên quan' section."""
    texts, images, videos = [], [], []
    skip_links = False
    for sib in h3.find_next_siblings():
        if sib.name in ("h2", "h3", "section"):
            break
        text = sib.get_text(" ", strip=True)
        if VIDEO_RE.match(text):
            videos.append(text)
            continue
        if text.startswith("Xem thêm"):
            skip_links = text.rstrip(": ") == "Xem thêm"
            continue
        if skip_links and is_link_list(sib):
            skip_links = False
            continue
        skip_links = False
        for img in sib.find_all("img"):
            src = img.get("src") or img.get("data-src")
            if src and not src.startswith("data:"):
                images.append(src)
        if text:
            texts.append(text)
    return "\n".join(texts), images, videos


CROSSREF = r"Xem\s+(?:thêm|chi\s+tiết)\s*:?\s*"
CROSSREF_LINE = re.compile(rf"^(?:Mẹo:\s*)?{CROSSREF}(?P<title>.*)$")
CROSSREF_TAIL = re.compile(rf"\s*{CROSSREF}(?P<title>[^.:!?]*)[.!]?$")


def clean_crossrefs(steps):
    """Remove 'Xem thêm: <title>' / 'Xem chi tiết: <title>' links to other articles from the step texts.

    The site renders these as plain text, often glued to the tip that follows ("...gan heo.Xem thêm:5 cách
    làm sạch gan heo không còn mùi hôiCật:..."). The same title also appears on a line of its own, so the
    titles are collected from those lines first and then cut out wherever they are glued to real text.
    """
    titles, kept = set(), []
    for step in steps:
        lines = []
        for line in step["text"].split("\n"):
            m = CROSSREF_LINE.match(line)
            if m:
                if m.group("title").strip():
                    titles.add(m.group("title").strip())
                continue
            lines.append(line)
        kept.append(lines)
    patterns = [re.compile(rf"\s*{CROSSREF}{re.escape(t)}") for t in sorted(titles, key=len, reverse=True)]
    for step, lines in zip(steps, kept):
        cleaned = []
        for line in lines:
            for pattern in patterns:
                line = pattern.sub("", line)
            line = CROSSREF_TAIL.sub("", line)  # a title that never had its own line, at the end of a tip
            line = re.sub(rf"\s*{CROSSREF}", " ", line).strip()  # a bare label left in the middle of text
            if line and line not in ("Mẹo:", "Mẹo: Mách nhỏ:"):
                cleaned.append(line)
        step["text"] = "\n".join(cleaned)
    return steps


def meta_value(soup, title):
    box = soup.find("div", attrs={"title": title})
    span = box.find("span") if box else None
    return span.get_text(strip=True) if span else None


def breadcrumb_category(soup):
    """(name, slug) of the recipe's category from the JSON-LD breadcrumb."""
    for script in soup.find_all("script", type="application/ld+json"):
        try:
            data = json.loads(script.string or "")
        except json.JSONDecodeError:
            continue
        if data.get("@type") == "BreadcrumbList":
            items = data.get("itemListElement", [])
            if len(items) >= 3:
                slug = (items[2].get("item") or "").rstrip("/").rsplit("/", 1)[-1] or None
                return items[2].get("name"), slug
    return None, None


def parse_recipe(url, html):
    soup = BeautifulSoup(html, "html.parser")
    h1 = soup.find("h1")
    if not h1:
        return None

    ingredients, steps, sections = [], [], []
    for h2 in soup.find_all("h2"):
        heading = h2.get_text(" ", strip=True)
        if heading.startswith("Nguyên liệu"):
            sections.append(heading)
            table = h2.find_next_sibling("table")
            if not table:
                continue
            for row in table.find_all("tr"):
                cells = [c.get_text(" ", strip=True) for c in row.find_all("td")]
                if len(cells) >= 2 and cells[0]:
                    name, note = split_name(cells[0])
                    ingredients.append({"name": name, "note": note, **parse_amount(cells[1]), "group": heading})

    videos = []
    for h3 in soup.find_all("h3"):
        title = h3.get_text(" ", strip=True)
        m = re.match(r"Bước\s*(\d+)\s*[:.]?\s*(.*)", title)
        if not m:
            continue
        text, images, step_videos = step_body(h3)
        videos += [v for v in step_videos if v not in videos]
        steps.append({
            "order": len(steps) + 1,
            "step_label": int(m.group(1)),
            "title": m.group(2),
            "text": text,
            "images": images,
        })

    if not ingredients or not steps:
        return None  # tips/articles, not a recipe
    clean_crossrefs(steps)

    category, category_slug = breadcrumb_category(soup)
    servings_text = meta_value(soup, "Phần ăn")
    servings_num = re.search(r"\d+", servings_text or "")
    og_image = soup.find("meta", property="og:image")
    desc = soup.find("meta", attrs={"name": "description"})
    return {
        "source": "angingon.com",
        "source_url": url,
        "slug": url.rstrip("/").rsplit("/", 1)[-1],
        "title": h1.get_text(" ", strip=True),
        "description": desc["content"] if desc else None,
        "category": category,
        "category_slug": category_slug,
        "prep_time_minutes": to_minutes(meta_value(soup, "Chuẩn bị")),
        "cook_time_minutes": to_minutes(meta_value(soup, "Nấu")),
        "servings_text": servings_text,
        "servings": int(servings_num.group()) if servings_num else None,
        "difficulty": meta_value(soup, "Độ khó"),
        "image_url": og_image["content"] if og_image else None,
        "video_url": videos[0] if videos else None,
        "ingredient_sections": sections,
        "ingredients": ingredients,
        "steps": steps,
        "scraped_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
    }


# ---------------------------------------------------------------- state

def read_jsonl(path):
    """(line number, record or None) for every non-empty line; None marks a line that is not valid JSON."""
    if not path.exists():
        return
    with path.open(encoding="utf-8") as f:
        for n, line in enumerate(f, 1):
            if line.strip():
                try:
                    yield n, json.loads(line)
                except json.JSONDecodeError:
                    yield n, None


def load_state(out_path, seen_path):
    done, per_category = set(), Counter()
    for _, rec in read_jsonl(out_path):
        if rec and "source_url" in rec:
            done.add(rec["source_url"])
            per_category[rec.get("category_slug")] += 1
    skipped = set(seen_path.read_text(encoding="utf-8").split()) if seen_path.exists() else set()
    return done, skipped, per_category


def scrape(args):
    out = Path(args.out)
    out.parent.mkdir(parents=True, exist_ok=True)
    seen_path = out.with_suffix(".skipped.txt")
    done, skipped, per_category = load_state(out, seen_path)
    print(f"Already have {len(done)} recipes, {len(skipped)} skipped pages.")

    fetcher = Fetcher(args.delay)
    slugs = all_categories(fetcher) if args.categories == "all" else args.categories.split(",")
    with out.open("a", encoding="utf-8") as fout, seen_path.open("a", encoding="utf-8") as fskip:
        for slug in slugs:
            have = per_category[slug]
            print(f"== {slug}: {have}/{args.per_category}")
            for url in category_urls(fetcher, slug):
                if have >= args.per_category:
                    break
                if url in done or url in skipped:
                    continue
                html = fetcher.get(url) if is_single_dish(url) else None
                recipe = parse_recipe(url, html) if html else None
                if recipe is None:
                    fskip.write(url + "\n")
                    fskip.flush()
                    skipped.add(url)
                    print(f"  - skip (not a single-dish recipe): {url}")
                    continue
                # Count the recipe under its own breadcrumb category so resuming stays consistent.
                fout.write(json.dumps(recipe, ensure_ascii=False) + "\n")
                fout.flush()
                done.add(url)
                per_category[recipe["category_slug"]] += 1
                have = per_category[slug]
                print(f"  [{slug} {have}/{args.per_category}] {recipe['title']}")
    print(f"Done: {len(done)} recipes in {out}")


def list_categories(args):
    fetcher = Fetcher(args.delay)
    done, _, per_category = load_state(Path(args.out), Path(args.out).with_suffix(".skipped.txt"))
    print(f"{'slug':<18} {'name':<22} {'on site':>8} {'have':>6}")
    for slug in all_categories(fetcher):
        _, name, total, last = category_page(fetcher, slug, 1)
        shown = total if total is not None else f"~{last * 24}"  # 24 recipes per listing page
        print(f"{slug:<18} {(name or '')[:22]:<22} {shown:>8} {per_category[slug]:>6}")


def validate(args):
    """Report lines that are not valid JSON, miss fields, or have malformed ingredients/steps."""
    problems, seen, count = [], {}, 0
    for n, rec in read_jsonl(Path(args.out)):
        count += 1
        if rec is None:
            problems.append(f"line {n}: not valid JSON")
            continue
        for field, kind in REQUIRED_FIELDS.items():
            if not isinstance(rec.get(field), kind):
                problems.append(f"line {n}: field '{field}' missing or not {kind.__name__}")
        url = rec.get("source_url")
        if url in seen:
            problems.append(f"line {n}: duplicate of line {seen[url]} ({url})")
        seen[url] = n
        for i, ing in enumerate(rec.get("ingredients") or [], 1):
            if not isinstance(ing, dict) or set(ing) != INGREDIENT_FIELDS:
                problems.append(f"line {n}: ingredient {i} has keys {sorted(ing) if isinstance(ing, dict) else ing}")
        for i, step in enumerate(rec.get("steps") or [], 1):
            if not isinstance(step, dict) or not (step.get("text") or step.get("images")):
                problems.append(f"line {n}: step {i} has neither text nor images")
            elif re.search(CROSSREF, step["text"]):
                problems.append(f"line {n}: step {i} still contains a 'Xem thêm/Xem chi tiết' link")
    for p in problems[:50]:
        print(p)
    if len(problems) > 50:
        print(f"... and {len(problems) - 50} more")
    print(f"{count} lines checked, {len(problems)} problems")
    sys.exit(1 if problems else 0)


def repair(args):
    """Re-apply the parsing rules that work on stored fields (amounts, ingredient notes, cross-links)."""
    src = Path(args.out)
    backup = src.with_suffix(".jsonl.bak")
    tmp = src.with_suffix(".jsonl.tmp")
    changed = bad = 0
    with tmp.open("w", encoding="utf-8") as fout:
        for n, rec in read_jsonl(src):
            if rec is None:
                bad += 1
                print(f"line {n}: not valid JSON, dropped")
                continue
            before = json.dumps(rec, ensure_ascii=False)
            for ing in rec.get("ingredients", []):
                name, note = split_name(ing.get("name", ""))
                ing.update(name=name, note=ing.get("note") or note, **parse_amount(ing.get("amount_text", "")))
            clean_crossrefs(rec.get("steps", []))
            after = json.dumps(rec, ensure_ascii=False)
            changed += before != after
            fout.write(after + "\n")
    src.replace(backup)
    tmp.replace(src)
    print(f"{changed} records changed, {bad} dropped; previous file kept as {backup}")


# ---------------------------------------------------------------- export

def export_csv(args):
    src = Path(args.out)
    outdir = src.parent
    recipes = [rec for _, rec in read_jsonl(src) if rec]
    with (outdir / "recipes.csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["slug", "title", "category", "category_slug", "prep_time_minutes", "cook_time_minutes",
                    "servings", "servings_text", "difficulty", "image_url", "source_url"])
        for r in recipes:
            w.writerow([r["slug"], r["title"], r["category"], r.get("category_slug"), r["prep_time_minutes"],
                        r["cook_time_minutes"], r["servings"], r["servings_text"], r["difficulty"],
                        r["image_url"], r["source_url"]])
    with (outdir / "recipe_ingredients.csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["slug", "position", "group", "name", "note", "quantity", "unit", "amount_text"])
        for r in recipes:
            for i, ing in enumerate(r["ingredients"], 1):
                w.writerow([r["slug"], i, ing["group"], ing["name"], ing.get("note"), ing["quantity"],
                            ing["unit"], ing["amount_text"]])
    with (outdir / "recipe_steps.csv").open("w", newline="", encoding="utf-8") as f:
        w = csv.writer(f)
        w.writerow(["slug", "order", "title", "text", "images"])
        for r in recipes:
            for s in r["steps"]:
                w.writerow([r["slug"], s["order"], s["title"], s["text"], " ".join(s["images"])])
    print(f"Wrote recipes.csv, recipe_ingredients.csv, recipe_steps.csv in {outdir} ({len(recipes)} recipes)")


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument("--categories", default="all",
                   help="comma-separated category slugs (see --list-categories), or 'all' (default)")
    p.add_argument("--per-category", type=int, default=60,
                   help="recipes to keep per category, counted by the recipe's own category (default 60)")
    p.add_argument("--delay", type=float, default=2.5, help="seconds between requests (default 2.5)")
    p.add_argument("--out", default="data/recipes.jsonl", help="output JSONL file")
    p.add_argument("--list-categories", action="store_true", help="show recipe counts per category and exit")
    p.add_argument("--validate", action="store_true", help="check the JSONL file and exit (status 1 on problems)")
    p.add_argument("--repair", action="store_true",
                   help="re-apply cleaning rules to the JSONL file without fetching (keeps a .bak copy)")
    p.add_argument("--export-csv", action="store_true", help="convert the JSONL file to CSV and exit")
    p.add_argument("--test-url", help="parse one recipe URL and print it (no file written)")
    args = p.parse_args()

    try:
        if args.validate:
            validate(args)
        elif args.repair:
            repair(args)
        elif args.export_csv:
            export_csv(args)
        elif args.list_categories:
            list_categories(args)
        elif args.test_url:
            fetcher = Fetcher(args.delay)
            print(json.dumps(parse_recipe(args.test_url, fetcher.get(args.test_url)), ensure_ascii=False, indent=2))
        else:
            scrape(args)
    except PermissionError as exc:
        print(f"Stopped: {exc}", file=sys.stderr)
        sys.exit(2)
    except KeyboardInterrupt:
        print("\nInterrupted. Run the same command again to resume.")


if __name__ == "__main__":
    main()
