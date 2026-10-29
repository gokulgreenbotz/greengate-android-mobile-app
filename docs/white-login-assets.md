# White luxury login assets

The white login is an additional saved choice under **Profile → Login Experience → White luxury login**. Existing dark and current-login choices retain their saved names. The login appearance also supplies the colors for the country picker and OTP screen; the signed-in home-screen theme is independent.

The built-in image-generation tool derived the architectural background from the user's white login reference and extracted the logo from their latest login screenshot. The white login and OTP screens display the replacement logo directly, with no tint, colour adjustment or added glow. Native Compose draws the tagline, progress accent, glass panel, fields and buttons.

- `app/src/main/res/drawable-nodpi/login_bg_luxury_white.png`: white marble architectural background without UI.
- `app/src/main/res/drawable-nodpi/greengate_white_brand_lockup.png`: transparent gold gate and dark bronze GreenGate Condo lettering extracted from the latest white login reference. Used only by the white login and OTP screens.
- `app/src/main/res/drawable-nodpi/greengate_brand_lockup.png`: original supplied logo retained for the dark login.

## Final background prompt

Use case: precise-object-edit. Asset type: full-screen Android login background, portrait 9:16. Use ONLY the user's white-and-gold architectural LOGIN screen reference among the supplied images (the one with GREENGATE CONDO, the gold G monogram, Welcome Back, mobile field and Continue); ignore the unrelated HOME DASHBOARD screenshots. Produce the clean background artwork from that login reference. Preserve the exact airy ivory-white marble curved wall, delicate sweeping champagne-gold architectural lines from the top left into the mid-right, warmly lit vertical gold wood slats along the left, green tropical plants at the lower left, and reflective pale marble floor. Match the reference's architectural framing: curve and plants around middle height, broad quiet ivory upper center for separate logo text; lower half pale reflective marble for a native login form. Remove ALL branding, letters, logo/monogram, tagline, progress bar, status-bar icons/time, login card/panel, fields, buttons, QR icon and all UI. Seamlessly reconstruct the background behind removed UI. No text, no logos, no UI, no watermark. Keep the premium luminous white/champagne palette and natural realism.

## Final logo extraction prompt

Built-in image tool, using the user-attached reference saved at `build/white-login-qa/user-white-logo-reference.png`. Selected output: `exec-605577b4-5002-4c2e-ab9c-21cfbec17b80.png`.

Use case: background-extraction. Make a faithful CUTOUT, not a redesigned logo. Use ONLY the attached source screenshot. The desired object occupies x=350..770 and y=160..445 in the 941x1672 source: the SMALL slender gold gate above the dark bronze serif GREENGATE wordmark, and CONDO with thin horizontal rules. Extract that exact complete lockup. Keep the screenshot's thin gold gate proportions, the exact letter forms/weight/spacing and relative size, its restrained original metallic shading, and the dark bronze wordmark colors exactly. DO NOT use the earlier large glowing logo. No changes to symbol, lettering, colors, material, weight, proportions or layout. No added glow, haze, light rays, shine, shadow, bevels or thicker 3D edges. Do not enlarge or embolden the gate relative to the words. Only remove the pale white background behind the logo, including inside counters. Center the cutout on a transparent landscape 3:2 canvas with small clear margins, retaining the same relative layout. Genuine transparent alpha background. Exclude ALL other screenshot elements: tagline, loading bar, gold architectural lines, wall, status bar and login UI. Exact text: GREENGATE and CONDO. No background rectangle.

