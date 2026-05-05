# Asset & Icon Pipeline

Handle images, icons, and illustrations when recreating screens from Figma/Stitch.

## Decision Tree

```
Is it an icon (small, monochrome, symbolic)?
  ├── Matches Material Icon? → Use Icons.Default.* or Icons.Outlined.*
  ├── Custom SVG? → Export → Convert to VectorDrawable
  └── Icon font (Stitch)? → Map name to Material Icons
Is it a photo or illustration?
  ├── Decorative/hero? → Placeholder Box with surfaceVariant
  ├── Content image? → AsyncImage with placeholder
  └── Background? → Box with background color/gradient
Is it a logo or brand asset?
  └── Use painterResource with placeholder drawable
```

## Icons

### Material Icons Mapping
Common Figma/Stitch icon names → Material equivalents:

| Source Name | Material Icon |
|------------|---------------|
| home, house | `Icons.Default.Home` |
| search, magnifier | `Icons.Default.Search` |
| settings, gear, cog | `Icons.Default.Settings` |
| person, user, avatar | `Icons.Default.Person` |
| arrow-back, chevron-left | `Icons.AutoMirrored.Default.ArrowBack` |
| arrow-forward, chevron-right | `Icons.AutoMirrored.Default.ArrowForward` |
| close, x | `Icons.Default.Close` |
| menu, hamburger | `Icons.Default.Menu` |
| add, plus | `Icons.Default.Add` |
| edit, pencil | `Icons.Default.Edit` |
| delete, trash | `Icons.Default.Delete` |
| favorite, heart | `Icons.Default.Favorite` |
| share | `Icons.Default.Share` |
| notification, bell | `Icons.Default.Notifications` |
| check, checkmark | `Icons.Default.Check` |
| info, info-circle | `Icons.Default.Info` |
| warning, alert | `Icons.Default.Warning` |
| error | `Icons.Default.Error` |
| star | `Icons.Default.Star` |
| lock | `Icons.Default.Lock` |
| mail, email | `Icons.Default.Email` |
| phone, call | `Icons.Default.Phone` |
| camera | `Icons.Default.CameraAlt` |
| download | `Icons.Default.Download` |
| upload | `Icons.Default.Upload` |
| filter | `Icons.Default.FilterList` |
| sort | `Icons.Default.Sort` |
| refresh | `Icons.Default.Refresh` |
| copy | `Icons.Default.ContentCopy` |
| link | `Icons.Default.Link` |
| calendar, date | `Icons.Default.DateRange` |
| clock, time | `Icons.Default.Schedule` |
| location, map-pin | `Icons.Default.LocationOn` |

### Custom SVG → VectorDrawable
When icon doesn't match Material:
1. Export from Figma: select icon → Export → SVG
2. Convert: Android Studio → File → New → Vector Asset → Local SVG
3. Or CLI: `android-svg-to-vd -i icon.svg -o res/drawable/ic_name.xml`
4. Name: `ic_{name}.xml` in `res/drawable/`
5. Use: `painterResource(R.drawable.ic_name)`

### Unknown Icons (placeholder)
```kotlin
Icon(
    imageVector = Icons.Default.Star, // TODO: Replace with actual icon
    contentDescription = "custom icon",
    tint = {Prefix}Theme.colors.onSurface,
)
```

## Images

### Placeholder Box (recommended for recreation)
```kotlin
// Photo/illustration placeholder
Box(
    modifier = Modifier
        .size(width.dp, height.dp)
        .background(
            {Prefix}Theme.colors.surfaceVariant,
            {Prefix}Theme.shapes.medium,
        )
        .clip({Prefix}Theme.shapes.medium),
    contentAlignment = Alignment.Center,
) {
    Icon(
        Icons.Default.Image,
        contentDescription = null,
        tint = {Prefix}Theme.colors.onSurfaceVariant,
        modifier = Modifier.size(24.dp),
    )
}
```

### Avatar Placeholder
```kotlin
Box(
    modifier = Modifier
        .size(48.dp)
        .background({Prefix}Theme.colors.primaryContainer, CircleShape)
        .clip(CircleShape),
    contentAlignment = Alignment.Center,
) {
    Icon(
        Icons.Default.Person,
        contentDescription = "avatar",
        tint = {Prefix}Theme.colors.onPrimaryContainer,
    )
}
```

### AsyncImage (when Coil available)
```kotlin
AsyncImage(
    model = "https://picsum.photos/300/200",
    contentDescription = "...",
    placeholder = ColorPainter({Prefix}Theme.colors.surfaceVariant),
    error = ColorPainter({Prefix}Theme.colors.errorContainer),
    contentScale = ContentScale.Crop,
    modifier = Modifier.size(width.dp, height.dp).clip(shape),
)
```

## Figma Image Layers

| Figma Layer Type | Compose |
|-----------------|---------|
| Rectangle with image fill (fill mode) | `Image(contentScale = ContentScale.Crop)` |
| Rectangle with image fill (fit mode) | `Image(contentScale = ContentScale.Fit)` |
| Ellipse with image fill | `Image(Modifier.clip(CircleShape))` |
| Background image full-bleed | `Box { Image(Modifier.matchParentSize(), contentScale = Crop); content }` |

## Stitch Image Elements

Stitch HTML `<img>` tags → Compose:
- `<img src="...">` → Placeholder Box (don't fetch external URLs)
- `<img>` with fixed dimensions → `Box(Modifier.size(w.dp, h.dp))` with placeholder
- `<img style="border-radius: 50%">` → Circular placeholder
- `<img style="object-fit: cover">` → note `ContentScale.Crop` for real implementation
