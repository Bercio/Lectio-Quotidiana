#!/usr/bin/env python3
"""
Quick test of the bibbiaedu.it scraping logic.
Mirrors the Kotlin BibleReferenceParser + BibleScraper logic exactly.
"""

import json
import re
import urllib.request
from datetime import date, timedelta

# ── Reference parser (mirrors BibleReferenceParser.kt) ───────────────────────

BOOK_RE = re.compile(r'^(\d?\s*[A-Za-z]+)\s*(.*)$')
NT_BOOKS = {
    "1Cor","1Gv","1Pt","1Tm","1Ts",
    "2Cor","2Gv","2Pt","2Tm","2Ts","3Gv",
    "Ap","At","Col","Eb","Ef","Fil","Fm","Gal","Gc","Gd","Gv",
    "Lc","Mc","Mt","Rm","Tt"
}

def url_path(abbrev):
    testament = "nt" if abbrev in NT_BOOKS else "at"
    return f"{testament}/{abbrev}"

def parse_range(chapter, spec):
    """Returns list of (chapter, start_verse, end_verse) spans."""
    hyphen = spec.find('-')
    if hyphen == -1:
        return [(chapter, spec, spec)]
    start = spec[:hyphen]
    end_spec = spec[hyphen+1:]
    comma = end_spec.find(',')
    if comma != -1:
        end_chapter = int(end_spec[:comma])
        end_verse = end_spec[comma+1:]
        return [(chapter, start, None), (end_chapter, None, end_verse)]
    # Bare letter suffix? e.g. "1a-r" → end becomes "1r"
    if end_spec.isalpha():
        end_spec = ''.join(c for c in start if c.isdigit()) + end_spec
    return [(chapter, start, end_spec)]

def parse_verse_spec(chapter, verse_spec):
    dot = verse_spec.find('.')
    if dot == -1:
        return parse_range(chapter, verse_spec)
    after_dot = verse_spec[dot+1:]
    if '-' in after_dot:
        # Non-consecutive: split at all dots
        spans = []
        for part in verse_spec.split('.'):
            spans.extend(parse_range(chapter, part))
        return spans
    else:
        # Cross-chapter dot: rewrite last dot as comma
        last_dot = verse_spec.rfind('.')
        rewritten = verse_spec[:last_dot] + ',' + verse_spec[last_dot+1:]
        return parse_range(chapter, rewritten)

def parse_chapter_and_verses(spec):
    if not spec:
        return []
    comma = spec.find(',')
    if comma == -1:
        return [(int(spec), None, None)]
    chapter = int(spec[:comma])
    return parse_verse_spec(chapter, spec[comma+1:])

def parse_reference(reference):
    """Returns list of (book, chapter, start_verse, end_verse)."""
    clean = reference.replace('*', '').strip()
    segments = re.split(r'[+;]', clean)
    result = []
    current_book = ''
    for seg in segments:
        seg = seg.strip()
        if not seg:
            continue
        m = BOOK_RE.match(seg)
        if m:
            current_book = re.sub(r'\s+', '', m.group(1))
            rest = m.group(2).strip()
            spans = parse_chapter_and_verses(rest)
        else:
            spans = parse_chapter_and_verses(seg)
        for (ch, sv, ev) in spans:
            result.append((current_book, ch, sv, ev))
    return result

# ── Verse label comparison (mirrors VerseLabel.kt) ───────────────────────────

def parse_label(label):
    """Returns (number, suffix) for comparison."""
    num = ''.join(c for c in label if c.isdigit())
    suf = ''.join(c for c in label if c.isalpha())
    return (int(num), suf) if num else None

def verse_in_span(label, start, end):
    v = parse_label(label)
    if v is None:
        return False
    if start is not None:
        s = parse_label(start)
        if s and v < s:
            return False
    if end is not None:
        e = parse_label(end)
        if e and v > e:
            return False
    return True

# ── Scraper (mirrors BibleScraper.kt) ────────────────────────────────────────

VERSE_RE = re.compile(
    r'<!--<sup>(\d+[a-z]*)</sup>-->(.*?)</span>',
    re.DOTALL
)
TAG_RE = re.compile(r'<[^>]+>')
ENTITY_RE = re.compile(r'&#(\d+);')
ENTITIES = {
    '&amp;': '&', '&lt;': '<', '&gt;': '>',
    '&quot;': '"', '&#039;': "'", '&nbsp;': ' ',
}

def clean_text(html):
    text = html.replace('<br>', ' ').replace('<br/>', ' ').replace('<br />', ' ')
    text = TAG_RE.sub('', text)
    for entity, char in ENTITIES.items():
        text = text.replace(entity, char)
    text = ENTITY_RE.sub(lambda m: chr(int(m.group(1))), text)
    return text.strip()

def fetch_chapter(book, chapter):
    path = url_path(book)
    url = f"https://www.bibbiaedu.it/CEI2008/{path}/{chapter}/"
    req = urllib.request.Request(url, headers={'User-Agent': 'CalendarioDossettiano/1.0'})
    with urllib.request.urlopen(req, timeout=10) as resp:
        html = resp.read().decode('utf-8')
    verses = {}
    for m in VERSE_RE.finditer(html):
        label = m.group(1)
        text = clean_text(m.group(2))
        if text:
            verses[label] = text
    return verses

# ── Main ─────────────────────────────────────────────────────────────────────

def get_reading(target_date: str):
    with open('calendario.json') as f:
        calendar = json.load(f)
    entry = next((e for e in calendar if e['data'] == target_date), None)
    if not entry:
        print(f"No reading found for {target_date}")
        return
    reference = entry['lectio']
    print(f"Date:      {target_date}")
    print(f"Reference: {reference}")
    print()

    passages = parse_reference(reference)
    # Group by (book, chapter) to avoid double-fetching
    seen = {}
    all_verses = []
    for (book, chapter, start, end) in passages:
        key = (book, chapter)
        if key not in seen:
            seen[key] = fetch_chapter(book, chapter)
        chapter_verses = seen[key]
        for label, text in chapter_verses.items():
            if verse_in_span(label, start, end):
                all_verses.append((book, chapter, label, text))

    for (book, chapter, label, text) in all_verses:
        print(f"{label}  {text}")

if __name__ == '__main__':
    tomorrow = (date.today() + timedelta(days=1)).isoformat()
    get_reading(tomorrow)
