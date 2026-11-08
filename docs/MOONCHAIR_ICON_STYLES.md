# Moonchair icon style groups

Long-press any native app/deep shortcut in the **homescreen or dock** and choose **Icon style** to assign one of four shared styles: Original, Neko (peach cat / plum), Hex (plum / pink), or Luna (lavender / plum).

Groups are persisted in two reserved high bits of each workspace shortcut's existing Favorites.options field. Source icon artwork and cached bitmap info are unchanged; styling happens when rendering the shortcut. Groups and colors can be made user-editable in a later iteration without changing assignments.

Limitations: this does not style Android widgets, including Shortcut Maker widgets, as those use AppWidgetHostView rather than BubbleTextView. Opaque square app icons may retain a square-looking monochrome glyph. Styling is unbuilt and needs device validation, particularly transitions, high-res icon reloads and long-press menus.
