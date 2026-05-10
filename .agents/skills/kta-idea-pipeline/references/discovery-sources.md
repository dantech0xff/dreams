# Discovery Sources (Phase 0)

WebFetch URL list for trigger path 5 (Trending Play Store) and supporting research. **No API keys** — public pages only.

## Sources

| Source | URL | Use For |
|---|---|---|
| Play Store top charts | https://play.google.com/store/apps/top | Trending app categories, top free / top grossing |
| Play Store new releases | https://play.google.com/store/apps/new | Recently launched, gaps in saturated cats |
| ProductHunt — mobile | https://www.producthunt.com/topics/mobile | New launches, validated pain points |
| ProductHunt — Android | https://www.producthunt.com/topics/android | Android-specific launches |
| Reddit r/androidapps | https://www.reddit.com/r/androidapps/top/?t=month | App discovery + complaints (read titles + top comments) |
| Reddit r/SideProject | https://www.reddit.com/r/SideProject/top/?t=month | Indie devs sharing what they shipped |
| IndieHackers — products | https://www.indiehackers.com/products | Indie revenue stories, patterns that work |
| IndieHackers — milestones | https://www.indiehackers.com/milestones | Recent launches with traction signals |

## Fetch Pattern

```
1. WebFetch {url}
2. If timeout / blocked / 4xx → retry once
3. If retry fails → emit fallback prompt-only flow (see below)
4. On success → extract: category labels, app names, recurring complaints, top-voted features
5. Synthesize 5–10 seed concepts using extracted signals
```

## Extraction Hints

- **Play Store** — note category badges and rank changes
- **ProductHunt** — read tagline + top comments (validation signal)
- **Reddit** — title patterns starting with "App that does X" or "Looking for an app that…"
- **IndieHackers** — revenue + niche pairings; copy the niche, not the product

## Fallback (when WebFetch fails)

Prompt the student instead:

```
WebFetch unavailable. Manual mode:
- Open Play Store top charts on your device
- List 5 apps in top 50 you've never heard of
- For each, what category? What seems to be the wedge?
- Tell me — I'll synthesize seed concepts.
```

Same output schema (5–10 seeds), different input source.

## Constraints

- **Never** scrape behind login walls — public pages only
- **Never** call paid APIs — WebFetch only
- **Always** state in seed output which source informed each idea (for traceability)
- **Update cadence** — sources may go behind anti-bot. If a source fails 3 runs in a row, flag for maintenance
