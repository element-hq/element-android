# Color migration

### Changes

- use colors defined in https://www.figma.com/file/X4XTH9iS2KGJ2wFKDqkyed/Compound?node-id=557%3A0
- remove unused resources and code (ex: PercentView)
- split some resource files into smaller file
- rework the theme files
- ensure material theme is used everywhere in the theme and in the layout
- add default style for some views in the theme (ex: Toolbar, etc.)
- add some debug screen in the debug menu, to test the themes and the button style
- rework the button style to use `materialThemeOverlay` attribute
- custom tint icon for menu management has been removed
- comment with `riotx` has been updated

### Main change for developers

- Read migration guide: https://github.com/vector-im/element-android/pull/3459/files#diff-f0e52729d5e4f6eccbcf72246807aa34ed19c4ef5625ca669df998cd1022874b
- Use MaterialAlertDialogBuilder instead of AlertDialog.Builder
- some Epoxy Item included a divider. This has been removed. Use a `dividerItem` or `bottomSheetDividerItem` Epoxy items to add a divider
- RecyclerView.configureWith now take a divider drawable instead of a divider color

### Remaining work

- Cleanup some vector drawables and ensure a tint is always used instead of hard coded color.

### Migration guide

Some colors and color attribute has been removed, here is the list and what has to be used now.

It can help Element Android forks maintainers to migrate their code.

- riotx_text_primary -> ?vctr_content_primary
- riotx_text_secondary -> ?vctr_content_secondary
- riotx_text_tertiary -> ?vctr_content_tertiary

- ?riotx_background -> ?android:colorBackground
- riotx_background_light -> element_background_light
- riotx_background_dark -> element_background_dark
- riotx_background_black -> element_background_black

- riotx_accent -> ?colorPrimary
- riotx_positive_accent -> ?colorPrimary
- riotx_accent_alpha25 -> color_primary_alpha25
- riotx_notice -> ?colorError
- riotx_destructive_accent -> ?colorError
- vector_error_color -> ?colorError
- vector_warning_color -> ?colorError

- riotx_bottom_sheet_background -> ?colorSurface
- riotx_alerter_background -> ?colorSurface

- riotx_username_1 -> element_name_01
- riotx_username_2 -> element_name_02
- riotx_username_3 -> element_name_03
- riotx_username_4 -> element_name_04
- riotx_username_5 -> element_name_05
- riotx_username_6 -> element_name_06
- riotx_username_7 -> element_name_07
- riotx_username_8 -> element_name_08

- riotx_avatar_fill_1 -> element_room_01
- riotx_avatar_fill_2 -> element_room_02
- riotx_avatar_fill_3 -> element_room_03

- white -> @android:color/white
- black -> @android:color/black or emoji_color

- riotx_list_header_background_color -> ?vctr_header_background
- riotx_header_panel_background -> ?vctr_header_background
- riotx_list_bottom_sheet_divider_color -> ?vctr_list_separator_on_surface
- riotx_list_divider_color -> ?vctr_list_separator
- list_divider_color -> ?vctr_list_separator
- riotx_header_panel_border_mobile -> ?vctr_list_separator
- riotx_bottom_nav_background_border_color -> ?vctr_list_separator
- riotx_header_panel_text_secondary -> ?vctr_content_primary

- link_color_light -> element_link_light
- link_color_dark -> element_link_dark

- riotx_toolbar_primary_text_color -> vctr_content_primary
- riotx_toolbar_secondary_text_color -> vctr_content_primary
- riot_primary_text_color -> vctr_content_primary

- riotx_android_secondary -> vctr_content_secondary
