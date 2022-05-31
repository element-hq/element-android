# Element Android design

<!--- TOC -->

* [Introduction](#introduction)
* [How to import from Figma to the Element Android project](#how-to-import-from-figma-to-the-element-android-project)
  * [Colors](#colors)
  * [Text](#text)
  * [Dimension, position and margin](#dimension-position-and-margin)
  * [Icons](#icons)
    * [Export drawable from Figma](#export-drawable-from-figma)
    * [Import in Android Studio](#import-in-android-studio)
  * [Images](#images)
* [Figma links](#figma-links)
  * [Coumpound](#coumpound)
  * [Login](#login)
    * [Login v2](#login-v2)
  * [Room list](#room-list)
  * [Timeline](#timeline)
  * [Voice message](#voice-message)
  * [Room settings](#room-settings)
  * [VoIP](#voip)
  * [Presence](#presence)
  * [Spaces](#spaces)
  * [List to be continued...](#list-to-be-continued)

<!--- END -->

## Introduction

Design at element.io is done using Figma - https://www.figma.com

## How to import from Figma to the Element Android project

Integration should be done using the Android development best practice, and should follow the existing convention in the code.

### Colors

Element Android already contains all the colors which can be used by the designer, in the module `ui-style`.
Some of them depend on the theme, so ensure to use theme attributes and not colors directly.

### Text

 - click on a text on Figma
 - on the right panel, information about the style and colors are displayed
 - in Element Android, text style are already defined, generally you should not create new style
 - apply the style and the color to the layout

### Dimension, position and margin

 - click on an item on Figma
 - dimensions of the item will be displayed.
 - move the mouse to other items to get relative positioning, margin, etc.

### Icons

#### Export drawable from Figma

 - click on the element to export
 - ensure that the correct layer is selected. Sometimes the parent layer has to be selected on the left panel
 - on the right panel, click on "export"
 - select SVG
 - you can check the preview of what will be exported
 - click on "export" and save the file locally
 - unzip the file if necessary

It's also possible for any icon to go to the main component by right-clicking on the icon.

#### Import in Android Studio

 - right click on the drawable folder where the drawable will be created
 - click on "New"/"Vector Asset"
 - select the exported file
 - update the filename if necessary
 - click on "Next" and click on "Finish"
 - open the created vector drawable
 - optionally update the color(s) to "#FF0000" (red) to ensure that the drawable is correctly tinted at runtime.

### Images

Android 4.3 (18+) fully supports the WebP image format which can often provide smaller image sizes without drastically impacting image quality (depending on the output encoding quality).
When importing non vector images, WebP is the preferred format.

Images can be converted to the WebP within Android Studio by
 - right clicking the image file within the project file explorer
 - select `Convert to WebP`

https://developer.android.com/studio/write/convert-webp

## Figma links

Figma links can be included in the layout, for future reference, but it is also OK to add a paragraph below here, to centralize the information

Main entry point: https://www.figma.com/files/project/5612863/Element?fuid=779371459522484071

Note: all the Figma links are not publicly available.

### Coumpound

Coumpound contains the theme of the application, with all the components, in Light and Dark theme: palette (colors), typography, iconography, etc.

https://www.figma.com/file/X4XTH9iS2KGJ2wFKDqkyed/Compound

### Login

TBD

#### Login v2

https://www.figma.com/file/xdV4PuI3DlzA1EiBvbrggz/Login-Flow-v2

### Room list

TBD

### Timeline

https://www.figma.com/file/x1HYYLYMmbYnhfoz2c2nGD/%5BRiotX%5D-Misc?node-id=0%3A1

### Voice message

https://www.figma.com/file/uaWc62Ux2DkZC4OGtAGcNc/Voice-Messages?node-id=473%3A12

### Room settings

TBD

### VoIP

https://www.figma.com/file/V6m2z0oAtUV1l8MdyIrAep/VoIP?node-id=4254%3A25767

### Presence

https://www.figma.com/file/qmvEskET5JWva8jZJ4jX8o/Presence---User-Status?node-id=114%3A9174
(Option B is chosen)

### Spaces

https://www.figma.com/file/m7L63aGPW7iHnIYStfdxCe/Spaces?node-id=192%3A30161

### List to be continued...
